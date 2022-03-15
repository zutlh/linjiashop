package cn.enilu.flash.api.controller;

import cn.enilu.flash.bean.constant.state.ManagerStatus;
import cn.enilu.flash.bean.core.AuthorizationUser;
import cn.enilu.flash.bean.entity.system.User;
import cn.enilu.flash.bean.vo.JwtUser;
import cn.enilu.flash.bean.vo.front.Rets;
import cn.enilu.flash.cache.CacheDao;
import cn.enilu.flash.core.log.LogManager;
import cn.enilu.flash.core.log.LogTaskFactory;
import cn.enilu.flash.security.UserService;
import cn.enilu.flash.service.system.AccountService;
import cn.enilu.flash.service.system.ManagerService;
import cn.enilu.flash.utils.HttpUtil;
import cn.enilu.flash.utils.MD5;
import cn.enilu.flash.utils.Maps;
import cn.enilu.flash.utils.StringUtil;
import cn.enilu.flash.web.controller.BaseController;
import org.nutz.mapl.Mapl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**

 */
@RestController
@RequestMapping("/account")
public class AccountController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Value("${jwt.token.expire.time}")
    private Long tokenExpireTime ;
    @Autowired
    private ManagerService managerService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;
    @Autowired
    private CacheDao cacheDao;
    /**
     * 用户登录<br>
     * 1，验证没有注册<br>
     * 2，验证密码错误<br>
     * 3，登录成功
     * @param userName
     * @param password
     * @return
     */
    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public Object login(@RequestParam("username") String userName,
                        @RequestParam("password") String password){
        try {
            logger.info("用户登录:" + userName + ",密码:" + password);
            //1,
            User user = managerService.findByAccount(userName);
            if (user == null) {
                return Rets.failure("该用户不存在");
            }
            if(user.getStatus() == ManagerStatus.FREEZED.getCode()){
                return Rets.failure("用户已冻结");
            }else if(user.getStatus() == ManagerStatus.DELETED.getCode()){
                return Rets.failure("用户已删除");
            }
            String passwdMd5 = MD5.md5(password, user.getSalt());
            //2,
            if (!user.getPassword().equals(passwdMd5)) {
                return Rets.failure("输入的密码错误");
            }

            String token = userService.loginForToken(new JwtUser(user));
            Map<String, String> result = new HashMap<>(1);
            logger.info("token:{}",token);
            result.put("token", token);
            LogManager.me().executeLog(LogTaskFactory.loginLog(user.getId(), HttpUtil.getIp()));
            return Rets.success(result);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Rets.failure("登录时失败");
    }

    /**
     * 退出登录
     * @return
     */
    @RequestMapping(value = "/logout",method = RequestMethod.POST)
    public Object logout(){
        HttpServletRequest request = HttpUtil.getRequest();
        String token = this.getToken(HttpUtil.getRequest());
        accountService.logout(token);
        Long idUser = getIdUser(request);
        LogManager.me().executeLog(LogTaskFactory.exitLog(idUser, HttpUtil.getIp()));
        return Rets.success();
    }

    @RequestMapping(value = "/info",method = RequestMethod.GET)
    public Object info( ){
        HttpServletRequest request = HttpUtil.getRequest();
        Long idUser = null;
        try {
            idUser = getIdUser(request);
        }catch (Exception e){
            return Rets.expire();
        }
        if(idUser!=null){
            User user =  managerService.get(idUser);
            if(StringUtil.isEmpty(user.getRoleid())){
                return Rets.failure("该用户未配置权限");
            }
            AuthorizationUser shiroUser = UserService.me().getAuthorizationInfo(user.getAccount());
            Map map = Maps.newHashMap("name",user.getName(),"role","admin","roles", shiroUser.getRoleCodes());
            map.put("permissions",shiroUser.getUrls());
            Map profile = (Map) Mapl.toMaplist(user);
            profile.put("dept",shiroUser.getDeptName());
            profile.put("roles",shiroUser.getRoleNames());
            map.put("profile",profile);

            return Rets.success(map);
        }
        return Rets.failure("获取用户信息失败");
    }
    @RequestMapping(value = "/updatePwd",method = RequestMethod.POST)
    public Object updatePwd( String oldPassword,String password, String rePassword){
        try {
            User user = managerService.get(getIdUser(HttpUtil.getRequest()));
            logger.info("oldPassword:{},password:{},rePassword:{}",MD5.md5(oldPassword, user.getSalt()),password,rePassword);

            if(!MD5.md5(oldPassword, user.getSalt()).equals(user.getPassword())){
                return Rets.failure("旧密码输入错误");
            }
            if(!password.equals(rePassword)){
                return Rets.failure("新密码前后不一致");
            }
            user.setPassword(MD5.md5(password, user.getSalt()));
            managerService.update(user);
            //清空缓存
            cacheDao.hset(CacheDao.SESSION,user.getAccount(),null);
            return Rets.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Rets.failure("更改密码失败");
    }

}
