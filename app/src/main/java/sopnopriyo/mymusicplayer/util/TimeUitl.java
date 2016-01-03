package sopnopriyo.mymusicplayer.util;

import java.text.SimpleDateFormat;

/**
 * Created by sopnopriyo on 2014/5/12.
 */
public class TimeUitl {
    public static String changeMillsToDateTime(int millsTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        return simpleDateFormat.format(millsTime);
    }
}
