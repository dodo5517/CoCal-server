package cola.springboot.cocal.common.util;

import eu.bitwalker.useragentutils.*;

public class DeviceInfoParser {
    public static String extractDeviceInfo(String userAgentString){
        if (userAgentString == null || userAgentString.isEmpty()){
            return "Unknown";
        }

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        OperatingSystem os = userAgent.getOperatingSystem();
        Browser browser = userAgent.getBrowser();

        String osName = os != null ? os.getName() : "Unknown OS"; // Android, iOS 등
        String deviceType = os != null ? os.getDeviceType().getName() : "Unknown DeviceType"; // Mobile, Tablet, Computer 등
        String browserName = browser != null ? browser.getName() : "Unknown Browser";

        // ex. Android Mobile Chrome
        return String.format("%s / %s / %s", osName, deviceType, browserName);
    }
}
