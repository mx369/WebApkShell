import { $ } from 'bun'
import { exec } from 'child_process'
import { copyFile } from 'fs/promises'
import { promisify } from 'util'

const getCfg = async () => {
    const { webApkShell } = await Bun.file('./package.json').json()
    return webApkShell as Partial<{
        build_script: 'npm run build'
        dist_path: 'dist'
        AP_ID: 'com.webapkshell.douyin'
        AP_NAME: 'WDY'
        AP_ICON: '/public/logo.xml'
        AP_VERSION_CODE: null
        AP_VERSION_NAME: ''
    }>
}

const { build_script, dist_path, AP_ICON, ...APS } = await getCfg()
build_script && (await promisify(exec)(build_script))

/**
 * 1. build生成dist
 * 2. 把dist移动到安卓项目内
 * 3. 把logo(如有)移动到安卓项目内
 * 4. 执行gradle打包命令,传递APS环境变量进去
 */
const main = async () => {
    await $`mkdir -p ./WebApkShell/app/src/main/res/drawable`
    await copyFile(`${AP_ICON}`, './WebApkShell/app/src/main/res/drawable/logo.xml').catch((e) => e)
    await $`rm -rf ./WebApkShell/app/src/main/assets`
    await $`mv ${dist_path} ./WebApkShell/app/src/main/assets/`
    await gradlew(APS)
}
main()

const gradlew = async (args: Record<string, any>) => {
    const env = {
        AP_VERSION_CODE: (Date.now() / 1000).toFixed(0),
        AP_VERSION_NAME: getVerName(),
        ...Object.fromEntries(Object.entries(args).filter(([_, v]) => v != null && v !== '')),
        AP_ICON: 'logo'
    }
    console.log('env', env)
    await $`./gradlew assembleRelease`
        .env({
            ...process.env,
            ...env
        })
        .cwd('./WebApkShell')
    $`exit 0`
}

function getVerName() {
    const tArr = new Date()
        .toString()
        .split(' ')[4]
        .split(':')
        .map((it) => it.padStart(2, '0'))
    tArr[2] = tArr[2]
        .split('')
        .map((n) => (+n + 10).toString(36))
        .join('')
        .toUpperCase()
    const date = new Date()
        .toLocaleDateString()
        .split('/')
        .map((it) => it.padStart(2, '0'))
        .sort((b, a) => a.length - b.length)
        .join('')
    return [date, tArr.join('')].join('T')
}