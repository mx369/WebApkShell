import { $ } from 'bun'
// 如果有配置logo,就复制logo
// build命令执行之后 把 dist目录复制到安卓项目
await $`./gradlew assembleRelease`.env({
    ...process.env,
    AP_ID: 'com.bzlaxy.todo',
    AP_NAME: 'TODO',
    AP_ICON: 'logo',
    AP_VERSION_CODE: (Date.now() / 1000).toFixed(0),
    AP_VERSION_NAME: getVerName()
})
$`exit 0`


function getVerName() {
    const tArr = new Date().toString().split(' ')[4].split(':').map(it => it.padStart(2, '0'))
    tArr[2] = tArr[2].split('').map(n => (+n + 10).toString(36))
        .join('').toUpperCase()
    const date = new Date().toLocaleDateString().split('/')
        .map(it => it.padStart(2, '0'))
        .sort((b, a) => a.length - b.length)
        .join('')
    return [date, tArr.join('')].join('T')
}
