## 特戒盒子

### 提交github
1. git config http.sslVerify "false"
2. git config --global http.proxy 127.0.0.1:7890
3. git config --global https.proxy 127.0.0.1:7890

### V3.0.8
1. 特戒余额变白嫖移至底部tab中间
2. 添加友盟统计的channel,用以区分后续通过电话邀请的用户,便于数据分析
3. 登录和获取版本号时有了渠道的区分,实现友盟统计和邀请用户奖励50元的弹框展示
4. 优化白嫖icon抖动触发多次后频频抖动问题
5. 标记邀请用户来源,展示在当前版本位置,样式为"版本号.1"
6. 友盟统计初始化位置的优化,由于新版APP不清楚下载地址来源也无法在APP初始化时直接初始化友盟,
   单拎出来一个工具类实现判断来源后,初始化友盟统计

### V3.0.6
1. 积分换成元
2. 积分使用说明提示
3. 限制获得和使用积分
4. 获取游戏礼包优化方式便于统计
5. 修复一些bug

### V3.0.5
1. 微信充值修复
2. 推广和51统计人数

### V3.0.3
1. 启动游戏走子进程
2. 更新进度条优化,字线分离
3. 新/红图标

### V3.0.2
1. 积分相关
   1.1 领积分: 签到,白嫖和分享
   1.2 花积分: 积分兑换充值
   1.3 乱七八糟的小红点和通知修改数据
2. 礼包码相关
   特定数字可以领取端游游戏的礼包码
3. 实名认证
   实名认证(可跳过)才可以进入游戏
4. 优化部分
   4.1 更新公告的优化展示
   4.2 在玩/收藏的游戏删除
   4.3 一键登录的双重验证请求
   4.4 更换了icon
   
### V3.0.1

### V3.0.0
为了适配手游版本
