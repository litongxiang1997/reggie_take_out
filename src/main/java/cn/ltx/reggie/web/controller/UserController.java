package cn.ltx.reggie.web.controller;

import cn.ltx.reggie.common.R;
import cn.ltx.reggie.entity.User;
import cn.ltx.reggie.service.UserService;
import cn.ltx.reggie.util.ValidateCodeUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @PROJECT_NAME: reggie_take_out
 * @PACKAGE_NAME: cn.ltx.reggie.web.controller
 * @CLASS_NAME: UserController
 * @Author: 21130
 * @CreateTime: 2022-08-27  19:58
 * @Description:
 * @Version: 1.0
 */
@RestController
@RequestMapping("/user")
@SuppressWarnings("ALL")
@Slf4j
public class UserController {
    @Autowired
    private UserService service;
    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取保存的验证码
        String codeInSession = session.getAttribute(phone).toString();

        //进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){
            //如果能够比对成功，说明登录成功

            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", phone);

            User user = service.getOne(wrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                service.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }
    @PostMapping("/loginout")
    public R<String> loginout(HttpServletRequest request) {
        //清理Session中保存的当前登录的id
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }

}
