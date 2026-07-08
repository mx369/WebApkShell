分分钟,把你的网页应用,打包为套壳安卓应用
使用教程:
1. 在你的项目添加当前项目为子项目
`git submodule add https://github.com/mx369/WebApkShell.git`
2. 添加流水线(windows系统手动复制)
`cp ./WebApkShell/workflows/android.yml .github/workflows/android.yml`
3. 在你的package.json里面添加apk配置
```json
"webApkShell": {
    "build_script": "pnpm run build:site",//项目打包命令
    "dist_path": "./packages/vant/site-dist",//打包后文件所在目录
    "AP_ID": "com.t.vant",//安卓应用包名
    "AP_ICON": "./public/logo.xml",//安卓应用icon,要矢量xml,不支持嵌套外挂资源,可以不写,有默认值;
    "AP_NAME": "VANT",//app的名称
    "AP_VERSION_NAME": "",
    "AP_SPLASH_TIMEOUT": "2000"//启动屏超时时间(毫秒),默认2000,JS可提前调用shell.dismissSplash()关闭
},

```
4. 修改公共基础路径为`./`
- 如vite.config.js 里面的 `base:'./'`
5. 其他可能需要修改的
a) package.json如果配置有"packageManager": "pnpm@10.18.3",需要删除,或者改流水线配置
b) 修改流水线的触发分支配置,默认为main
c) 安卓应用图标生成攻略:
    - svg转xml https://svg2vector.com/
    - svg素材网址 https://www.iconfont.cn/collections/index
d) JS 接口 (window.shell)
```js
// 1. 获取状态栏高度
const statusBarHeight = window.shell?.statusBarHeight || '0px';
// 可以通过css变量
document.body.style.setProperty('--bar-height', statusBarHeight);

// 2. 关闭启动屏
// 首页数据加载完成后调用,提前关闭启动屏;不调用则等 AP_SPLASH_TIMEOUT 超时后自动关闭
window.shell?.dismissSplash();
```


上面的操作执行完成,提交代码到GitHub,就会自动打包项目为APK
在你的项目首页;右侧的release处,就能下载你的apk了