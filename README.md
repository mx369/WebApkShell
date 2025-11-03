构建流程:
1.在package.json里面配置{
    "build_script": "pnpm run build",
    "dist_path": "./dist",
    AP_ID: 'com.webapkshell.tmp',//非必填,应用包名
    AP_NAME: 'webapkshell',//非必填,应用名称
    AP_ICON: './public/logo.xml',//非必填,安卓xml图标路径(可以使用安卓studio 从svg转为xml)
    AP_VERSION_CODE: 88,// 非必填,会自动生成 (Date.now() / 1000).toFixed(0),
    AP_VERSION_NAME: '88.88'//版本号名称(在安装的时候会显示) 非必填,会自动生成 getVerName()
}
2. 复制流水线,复制genApk.ts
3. 添加子仓库 https://github.com/mx369/WebApkShell.git

