package com.wjj.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.wjj.common.Const;
import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.mq.MqProducer;
import com.wjj.response.CommonReturnType;
import com.wjj.service.IItemService;
import com.wjj.service.IOrderService;
import com.wjj.service.IPromoService;
import com.wjj.service.model.OrderModel;
import com.wjj.service.model.UserModel;
import com.wjj.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 22:26
 */
@Controller
@RequestMapping("/order")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class OrderController {

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private IItemService iItemService;

    @Autowired
    private IPromoService iPromoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        executorService= Executors.newFixedThreadPool(20);
        orderCreateRateLimiter=RateLimiter.create(50);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam("itemId") Integer itemId,
                                        @RequestParam(value = "promoId") Integer promoId,
                                          @RequestParam(value = "verifyCode")String verifyCode) throws BusinessException {
        //获取登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isBlank(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }
        //通过verifycode验证验证码的有效性
        String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isBlank(redisVerifyCode)|| !StringUtils.equalsIgnoreCase(redisVerifyCode,verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法，验证码错误");
        }
        //获取秒杀访问令牌
        String promoToken = iPromoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (promoToken==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }
        //返回对应的结果
        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/generateverifycode")
    @ResponseBody
    public void generateverifycode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isBlank(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }

        //创建文件输出流对象
        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(),10,TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
    }

    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId",required = false) Integer promoId,
                                        @RequestParam(value = "promoToken",required = false)String promoToken) throws BusinessException {
        if (orderCreateRateLimiter.acquire()<=0){
            throw new BusinessException(EmBusinessError.RATE_LIMIT);
        }
        //获取登录信息
     //   Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isBlank(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }
        //获取登录信息

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }

        //校验秒杀令牌是否正确
        if (promoId!=null){
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId);
            if (inRedisPromoToken==null||!StringUtils.equals(promoToken,inRedisPromoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }

        }
        //同步调用线程池的submit方法,同1时间只有20个线程能执行
        //拥塞窗口为20的等待队列，用来队列化泄洪
        //一台服务器只有1个executorService，同1时间只有20个请求能执行，其它请求都要排队
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水init状态
                String stockLogId = iItemService.initStockLog(itemId, amount);

                //再去完成对应的下单事务型消息机制
                boolean mqResult = mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId);
                if (!mqResult) {
                    throw new BusinessException(EmBusinessError.UNKOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try {
            //  获取任务执行结果，任务结束之前会阻塞。
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new BusinessException(EmBusinessError.UNKOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKOWN_ERROR);
        }
        return CommonReturnType.create(null);
    }
}
