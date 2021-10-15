export const currentOs = () => {
    const isCurrentOsEqual = (osShortName) => {
        if(typeof window !== "undefined") {
            return window.navigator.userAgent.indexOf(osShortName) !== -1
        }
        return false
    }
    if(isCurrentOsEqual("Win")) return "win"
    if(isCurrentOsEqual("Mac")) return "mac"
    else return "linux"
}