构建流程:
1.在package.json里面配置{
    AP_ID: 'com.bzlaxy.todo',//非必填,应用包名
    AP_NAME: 'TODO',//非必填,应用名称
    AP_ICON: 'logo',//非必填,安卓xml图标路径(可以使用安卓studio 从svg转为xml)
    AP_VERSION_CODE: 88,// 非必填,会自动生成 (Date.now() / 1000).toFixed(0),
    AP_VERSION_NAME: '88.88'//版本号名称(在安装的时候会显示) 非必填,会自动生成 getVerName()
}
2. 把你的web项目打包到xx目录
3. 把webapkshell拉到你的项目
4. 执行npm run genApk
