const app = getApp();
const request = require('../../utils/request.js');
const util = require('../../utils/util.js');

Page({
  data: {
    userInfo: {},
    defaultAvatar: 'https://via.placeholder.com/200/4CAF50/FFFFFF?text=User',
    roleName: '',
    roleClass: 'consumer',
    isEnterprise: false,
    enterpriseTypeName: '',
    auditStatus: 0,
    auditStatusText: '',
    hasChanges: false,
    
    // 编辑昵称
    showNicknameModal: false,
    tempNickname: '',
    
    // 编辑电话
    showPhoneModal: false,
    tempPhone: '',
    phoneError: '',
    
    // 修改密码
    showPasswordModal: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    showOldPassword: false,
    showNewPassword: false,
    showConfirmPassword: false,
    passwordError: '',
    
    // 原始数据（用于比较是否有修改）
    originalData: {}
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  loadUserInfo() {
    // 从全局获取用户信息
    const userInfo = app.globalData.userInfo || {};
    const userType = userInfo.userType || 3;
    
    // 解析头像URL：先不设置到data，等proxyImage完成后再显示
    // 微信真机<image>无法加载http://局域网URL，必须先下载为本地临时文件
    let avatarHttpUrl = userInfo.avatar || '';
    if (avatarHttpUrl && !avatarHttpUrl.startsWith('http')) {
      const baseUrl = (app.globalData && app.globalData.apiBaseUrl) || '';
      avatarHttpUrl = baseUrl + avatarHttpUrl;
    }

    const fullUserInfo = {
      username: userInfo.username || '',
      realName: userInfo.realName || '',
      phone: userInfo.phone || '',
      avatar: avatarHttpUrl,
      enterpriseName: userInfo.enterpriseName || '',
      enterpriseType: userInfo.enterpriseType || 0,
      createTime: userInfo.createTime || '',
      ...userInfo,
      avatar: avatarHttpUrl
    };
    
    // 角色信息
    let roleName = '普通用户';
    let roleClass = 'consumer';
    if (userType === 1) {
      roleName = '系统管理员';
      roleClass = 'admin';
    } else if (userType === 2) {
      roleName = '企业用户';
      roleClass = 'enterprise';
    }
    
    // 企业类型
    const enterpriseTypeMap = {
      1: '种植养殖企业',
      2: '加工宰杀企业',
      3: '检疫质检企业'
    };
    
    // 审核状态
    const auditStatusMap = {
      0: '审核中',
      1: '已通过',
      2: '已拒绝'
    };
    
    const auditStatus = userInfo.auditStatus || 1;
    
    this.setData({
      userInfo: fullUserInfo,
      roleName,
      roleClass,
      isEnterprise: userType === 2,
      enterpriseTypeName: enterpriseTypeMap[fullUserInfo.enterpriseType] || '未设置',
      auditStatus,
      auditStatusText: auditStatusMap[auditStatus],
      originalData: {
        realName: fullUserInfo.realName,
        phone: fullUserInfo.phone,
        avatar: fullUserInfo.avatar
      }
    });

    // 代理http头像图片为本地临时文件（真机兼容，开发者工具可直接加载http）
    if (avatarHttpUrl) {
      util.proxyImage(avatarHttpUrl).then(lp => {
        if (lp && lp !== avatarHttpUrl) {
          this.setData({ 'userInfo.avatar': lp });
        }
      });
    }
  },

  // 选择头像
  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...', mask: true });
        const submitHelper = require('../../utils/submit-helper.js');
        submitHelper.uploadImage(tempFilePath, 'avatar').then(serverUrl => {
          // serverUrl是相对路径如/uploads/avatar/xxx.jpg，拼接完整URL用于显示
          let displayUrl = serverUrl;
          if (serverUrl && !serverUrl.startsWith('http')) {
            const baseUrl = (app.globalData && app.globalData.apiBaseUrl) || '';
            displayUrl = baseUrl + serverUrl;
          }
          this.setData({ 'userInfo.avatar': displayUrl });
          // 代理http图片（真机兼容）
          util.proxyImage(displayUrl).then(lp => {
            if (lp && lp !== displayUrl) {
              this.setData({ 'userInfo.avatar': lp });
            }
          });

          // 自动保存avatar到后端，不需要用户手动点保存
          const userId = this.data.userInfo.id || (app.globalData.userInfo && app.globalData.userInfo.id);
          if (userId) {
            request.put('/api/system/user/update', {
              id: userId,
              avatar: serverUrl
            }).then(updateRes => {
              wx.hideLoading();
              if (updateRes.code === 200) {
                // 同步更新globalData和storage
                app.globalData.userInfo = {
                  ...app.globalData.userInfo,
                  avatar: serverUrl
                };
                wx.setStorageSync('userInfo', app.globalData.userInfo);
                wx.showToast({ title: '头像已更新', icon: 'success' });
              } else {
                wx.showToast({ title: updateRes.message || '保存失败', icon: 'none' });
              }
            }).catch(() => {
              wx.hideLoading();
              wx.showToast({ title: '头像保存失败', icon: 'none' });
            });
          } else {
            wx.hideLoading();
            wx.showToast({ title: '头像已更新', icon: 'success' });
          }
        }).catch(() => {
          wx.hideLoading();
          wx.showToast({ title: '头像上传失败', icon: 'none' });
        });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '选择失败', icon: 'none' });
        }
      }
    });
  },

  // 检查是否有修改（头像已在上传时自动保存，此处只检查昵称和电话）
  checkChanges() {
    const { userInfo, originalData } = this.data;
    const hasChanges = 
      userInfo.realName !== originalData.realName ||
      userInfo.phone !== originalData.phone;
    
    this.setData({ hasChanges });
  },

  // 编辑昵称
  editNickname() {
    this.setData({
      showNicknameModal: true,
      tempNickname: this.data.userInfo.realName || ''
    });
  },

  closeNicknameModal() {
    this.setData({
      showNicknameModal: false,
      tempNickname: ''
    });
  },

  onNicknameInput(e) {
    this.setData({
      tempNickname: e.detail.value
    });
  },

  confirmNickname() {
    const { tempNickname } = this.data;
    if (!tempNickname.trim()) {
      wx.showToast({
        title: '昵称不能为空',
        icon: 'none'
      });
      return;
    }
    
    this.setData({
      'userInfo.realName': tempNickname.trim(),
      showNicknameModal: false,
      tempNickname: ''
    });
    this.checkChanges();
  },

  // 编辑电话
  editPhone() {
    this.setData({
      showPhoneModal: true,
      tempPhone: this.data.userInfo.phone || '',
      phoneError: ''
    });
  },

  closePhoneModal() {
    this.setData({
      showPhoneModal: false,
      tempPhone: '',
      phoneError: ''
    });
  },

  onPhoneInput(e) {
    this.setData({
      tempPhone: e.detail.value,
      phoneError: ''
    });
  },

  confirmPhone() {
    const { tempPhone } = this.data;
    const phoneReg = /^1[3-9]\d{9}$/;
    
    if (!tempPhone) {
      this.setData({ phoneError: '请输入手机号码' });
      return;
    }
    
    if (!phoneReg.test(tempPhone)) {
      this.setData({ phoneError: '请输入正确的手机号码' });
      return;
    }
    
    this.setData({
      'userInfo.phone': tempPhone,
      showPhoneModal: false,
      tempPhone: '',
      phoneError: ''
    });
    this.checkChanges();
  },

  // 修改密码
  changePassword() {
    this.setData({
      showPasswordModal: true,
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      passwordError: ''
    });
  },

  closePasswordModal() {
    this.setData({
      showPasswordModal: false,
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      passwordError: '',
      showOldPassword: false,
      showNewPassword: false,
      showConfirmPassword: false
    });
  },

  onOldPasswordInput(e) {
    this.setData({
      oldPassword: e.detail.value,
      passwordError: ''
    });
  },

  onNewPasswordInput(e) {
    this.setData({
      newPassword: e.detail.value,
      passwordError: ''
    });
  },

  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value,
      passwordError: ''
    });
  },

  toggleOldPassword() {
    this.setData({ showOldPassword: !this.data.showOldPassword });
  },

  toggleNewPassword() {
    this.setData({ showNewPassword: !this.data.showNewPassword });
  },

  toggleConfirmPassword() {
    this.setData({ showConfirmPassword: !this.data.showConfirmPassword });
  },

  confirmPassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data;
    
    if (!oldPassword) {
      this.setData({ passwordError: '请输入原密码' });
      return;
    }
    
    if (!newPassword) {
      this.setData({ passwordError: '请输入新密码' });
      return;
    }
    
    if (newPassword.length < 6 || newPassword.length > 20) {
      this.setData({ passwordError: '新密码长度应为6-20位' });
      return;
    }
    
    if (newPassword !== confirmPassword) {
      this.setData({ passwordError: '两次输入的密码不一致' });
      return;
    }
    
    if (oldPassword === newPassword) {
      this.setData({ passwordError: '新密码不能与原密码相同' });
      return;
    }
    
    wx.showLoading({ title: '提交中...' });
    
    request.put('/api/system/user/change-password', {
      oldPassword: oldPassword,
      newPassword: newPassword
    }).then(res => {
      wx.hideLoading();
      if (res.code === 200) {
        this.closePasswordModal();
        wx.showModal({
          title: '修改成功',
          content: '密码已修改，请使用新密码重新登录',
          showCancel: false,
          success: () => {
            app.globalData.token = '';
            app.globalData.userInfo = {};
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
            wx.redirectTo({ url: '/pages/login/login' });
          }
        });
      } else {
        this.setData({ passwordError: res.message || '密码修改失败' });
      }
    }).catch(() => {
      wx.hideLoading();
      this.setData({ passwordError: '密码修改失败，请稍后再试' });
    });
  },

  // 保存个人信息
  saveProfile() {
    const { userInfo, hasChanges } = this.data;
    
    if (!hasChanges) {
      wx.showToast({
        title: '没有修改内容',
        icon: 'none'
      });
      return;
    }
    
    wx.showLoading({ title: '保存中...' });
    
    const userId = userInfo.id || (app.globalData.userInfo && app.globalData.userInfo.id);
    if (!userId) {
      wx.hideLoading();
      wx.showToast({ title: '用户信息异常', icon: 'none' });
      return;
    }
    
    request.put('/api/system/user/update', {
      id: userId,
      realName: userInfo.realName,
      phone: userInfo.phone
    }).then(res => {
      wx.hideLoading();
      if (res.code === 200) {
        // 更新全局用户信息
        app.globalData.userInfo = {
          ...app.globalData.userInfo,
          realName: userInfo.realName,
          phone: userInfo.phone
        };
        wx.setStorageSync('userInfo', app.globalData.userInfo);
        this.setData({
          hasChanges: false,
          originalData: {
            realName: userInfo.realName,
            phone: userInfo.phone,
            avatar: userInfo.avatar
          }
        });
        wx.showToast({ title: '保存成功', icon: 'success' });
      } else {
        wx.showToast({ title: res.message || '保存失败', icon: 'none' });
      }
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存失败', icon: 'none' });
    });
  }
});
