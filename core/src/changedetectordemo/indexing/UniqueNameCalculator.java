package changedetectordemo.indexing;

import java.io.File;

import com.helospark.lightdi.annotation.Component;

@Component
public class UniqueNameCalculator {

    public String calculateUniqueId(File file) {
//        byte[] firstMegabyte = new byte[1024];
//        try (FileInputStream fio = new FileInputStream(file)) {
//            fio.read(firstMegabyte);
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            return toHexString(md5.digest(firstMegabyte)) + "_" + file.length();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        return file.getName() + "_" + file.length();
    }
//
//    public String toHexString(byte[] bytes) {
//        StringBuilder hexString = new StringBuilder();
//
//        for (int i = 0; i < bytes.length; i++) {
//            String hex = Integer.toHexString(0xFF & bytes[i]);
//            if (hex.length() == 1) {
//                hexString.append('0');
//            }
//            hexString.append(hex);
//        }
//
//        return hexString.toString();
//    }

}
