package data.http.key;

import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;
import ui.Main;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static system.application.SupportService.*;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 21/10/2017.
 */
public class CipherUtils {
    public final static String API_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyoET+mUijKlhWe0TiOOZ\n" +
            "XsqmT3YkKWxHjZmYkM6+hqv2pwMC9i8/rCJQKdIe1YAq+s1b2RHKX4qdFt410hZ+\n" +
            "3gwgCAS8iV+mBqwIYpHT2truMj3lIKmrvw+OIqgbef0zUpJjhes+0+gH41Mr1jtJ\n" +
            "euQ1/uBiJD1W9e4SHBZqVXltnEmmP9STfrOFOrrd7BsNDUdNgHrjZUCWW0B/NSHr\n" +
            "gXu+hunIloozNQoV9xJi5Jwf6TfxlM3QfiSVlkmP4FDftSAkJ/F7vBGpwNyoRCCW\n" +
            "KmBJDNwPAwZc5X3NLV7RqOUVM62ABAr6Pi3TQQPHvNQbXfKtD66FCTuxnSAkn0+g\n" +
            "swIDAQAB\n";

    public static PublicKey loadPublicKey() throws Exception {

        // strip of newlines, whitespaces
        String publicKeyPEM = API_PUB_KEY.replaceAll("\\s", "");

        // decode to get the binary DER representation
        byte[] publicKeyDER = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyDER));
    }

    private static String cipher(String clearText, Key key, String algo) throws Exception {
        Cipher cipher = Cipher.getInstance(algo);

        if (algo.equals("AES/CBC/PKCS5PADDING")) {
            byte[] ivBytes = DatatypeConverter.parseBase64Binary(getEncryptionIV());
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivBytes));
            byte[] encBytes = cipher.doFinal(clearText.getBytes("UTF-8"));
            // concat iv + encripted bytes
            byte[] concat = new byte[ivBytes.length + encBytes.length];
            System.arraycopy(ivBytes, 0, concat, 0, ivBytes.length);
            System.arraycopy(encBytes, 0, concat, ivBytes.length, encBytes.length);
            return DatatypeConverter.printBase64Binary(concat);
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        }
    }

    public static String cipherRSA(String clearText, SecretKey keyRSA) throws Exception {
        return cipher(clearText, keyRSA, "RSA/ECB/PKCS1Padding");
    }

    public static String cipherAESKeyWithRSA(SecretKey keyAES, Key keyRSA) throws Exception {
        return cipher(Base64.getEncoder().encodeToString(keyAES.getEncoded()), keyRSA, "RSA/ECB/PKCS1Padding");
    }

    public static String cipherAES(JSONObject obj, SecretKey keyAES) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("JSONObject cannot be null !");
        }
        String key_salt = "salt_" + RandomStringUtils.randomAlphanumeric(10);
        obj.put(key_salt, RandomStringUtils.randomAlphanumeric(20));
        return cipher(obj.toString(), keyAES, "AES/CBC/PKCS5PADDING");
    }

    public static SecretKey generateAES128Key() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // for example
        return keyGen.generateKey();
    }

    private static String getEncryptionIV() {
        SecureRandom random = new SecureRandom();
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        return DatatypeConverter.printBase64Binary(ivBytes);
    }
}
