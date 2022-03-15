package cn.enilu.flash.api.controller.system;

import cn.enilu.flash.bean.constant.Const;
import cn.enilu.flash.bean.constant.factory.PageFactory;
import cn.enilu.flash.bean.constant.state.ManagerStatus;
import cn.enilu.flash.bean.core.BussinessLog;
import cn.enilu.flash.bean.dto.UserDto;
import cn.enilu.flash.bean.entity.system.User;
import cn.enilu.flash.bean.enumeration.Permission;
import cn.enilu.flash.bean.exception.ApplicationException;
import cn.enilu.flash.bean.exception.ApplicationExceptionEnum;
import cn.enilu.flash.bean.vo.front.Rets;
import cn.enilu.flash.bean.vo.query.SearchFilter;
import cn.enilu.flash.core.factory.UserFactory;
import cn.enilu.flash.service.system.LogObjectHolder;
import cn.enilu.flash.service.system.ManagerService;
import cn.enilu.flash.utils.BeanUtil;
import cn.enilu.flash.utils.MD5;
import cn.enilu.flash.utils.RandomUtil;
import cn.enilu.flash.utils.StringUtil;
import cn.enilu.flash.utils.factory.Page;
import cn.enilu.flash.warpper.UserWarpper;
import cn.enilu.flash.web.controller.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * UserController
 *
 * @author enilu
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {
    @Autowired
    private ManagerService managerService;
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @RequiresPermissions(value = {Permission.USER})
    public Object list(@RequestParam(required = false) String account,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Integer sex){
        Page page = new PageFactory().defaultPage();
        if(StringUtil.isNotEmpty(name)){
            page.addFilter(SearchFilter.build("name", SearchFilter.Operator.LIKE, name));
        }
        if(StringUtil.isNotEmpty(account)){
            page.addFilter(SearchFilter.build("account", SearchFilter.Operator.LIKE, account));
        }
        page.addFilter( "status",SearchFilter.Operator.GT,0);
        page.addFilter("sex", sex);
        page = managerService.queryPage(page);
        List list = (List) new UserWarpper(BeanUtil.objectsToMaps(page.getRecords())).warp();
        page.setRecords(list);
        return Rets.success(page);
    }
    @RequestMapping(method = RequestMethod.POST)
    @BussinessLog(value = "编辑管理员", key = "name")
    @RequiresPermissions(value = {Permission.USER_EDIT})
    public Object save( @Valid UserDto user){
        if(user.getId()==null) {
            // 判断账号是否重复
            User theUser = managerService.findByAccount(user.getAccount());
            if (theUser != null) {
                throw new ApplicationException(ApplicationExceptionEnum.USER_ALREADY_REG);
            }
            if(user.getPassword()==null){
                return Rets.failure("密码不能为空");
            }
            // 完善账号信息
            user.setSalt(RandomUtil.getRandomString(5));
            user.setPassword(MD5.md5(user.getPassword(), user.getSalt()));
            user.setStatus(ManagerStatus.OK.getCode());
            managerService.insert(UserFactory.createUser(user, new User()));
        }else{

            User oldUser = managerService.get(user.getId());
            LogObjectHolder.me().set(oldUser);
            managerService.update(UserFactory.updateUser(user,oldUser));
        }
        return Rets.success();
    }

    @BussinessLog(value = "删除管理员", key = "userId")
    @RequestMapping(method = RequestMethod.DELETE)
    @RequiresPermissions(value = {Permission.USER_DEL})
    public Object remove(@RequestParam Long userId){
        if (userId == null) {
            throw new ApplicationException(ApplicationExceptionEnum.REQUEST_NULL);
        }
        if(userId.intValue()<=2){
            return Rets.failure("不能删除初始用户");
        }
        User user = managerService.get(userId);
        user.setStatus(ManagerStatus.DELETED.getCode());
        managerService.update(user);
        return Rets.success();
    }
    @BussinessLog(value="设置用户角色",key="userId")
    @RequestMapping(value="/setRole",method = RequestMethod.GET)
    @RequiresPermissions(value = {Permission.USER_EDIT})
    public Object setRole(@RequestParam("userId") Long userId, @RequestParam("roleIds") String roleIds) {
        if (BeanUtil.isOneEmpty(userId, roleIds)) {
            throw new ApplicationException(ApplicationExceptionEnum.REQUEST_NULL);
        }
        //不能修改超级管理员
        if (userId.equals(Const.ADMIN_ID)) {
            throw new ApplicationException(ApplicationExceptionEnum.CANT_CHANGE_ADMIN);
        }
        User user = managerService.get(userId);
        user.setRoleid(roleIds);
        managerService.update(user);
        return Rets.success();
    }

}
