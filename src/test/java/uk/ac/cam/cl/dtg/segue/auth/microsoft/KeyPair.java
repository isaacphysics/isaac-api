package uk.ac.cam.cl.dtg.segue.auth.microsoft;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;

public class KeyPair {
    private final java.security.KeyPair keyPair;

    public KeyPair() {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public PublicKey getPublic() {
        return new PublicKey(keyPair.getPublic());
    }

    public RSAPrivateKey getPrivate() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }
}
