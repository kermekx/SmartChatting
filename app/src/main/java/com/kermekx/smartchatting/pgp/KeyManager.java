package com.kermekx.smartchatting.pgp;

import com.kermekx.smartchatting.hash.Hasher;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.util.encoders.Base64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by kermekx on 03/03/2016.
 * s
 */
public class KeyManager {

    public static boolean generateKeys(String id, String password, String pin, ByteArrayOutputStream publicKey, ByteArrayOutputStream privateKey) {
        byte[] pass = new BigInteger(Hasher.md5Byte(password)).multiply(new BigInteger(Hasher.sha256Byte(pin))).toByteArray();

        try {
            PGPKeyRingGenerator generator = generateKeyRingGenerator(id, new String(pass).toCharArray());

            Base64Encoder encoder = new Base64Encoder();

            ByteArrayOutputStream os = new ByteArrayOutputStream(2048);
            PGPPublicKeyRing pkr = generator.generatePublicKeyRing();
            pkr.encode(os);
            byte[] bytes = os.toByteArray();
            os.close();
            encoder.encode(bytes, 0, bytes.length, publicKey);

            ByteArrayOutputStream sos = new ByteArrayOutputStream(4096);
            PGPSecretKeyRing skr = generator.generateSecretKeyRing();
            skr.encode(sos);
            byte[] sbytes = sos.toByteArray();
            encoder.encode(sbytes, 0, sbytes.length, privateKey);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static PGPKeyRingGenerator generateKeyRingGenerator(String id, char[] pass) throws Exception {
        return generateKeyRingGenerator(id, pass, 0xc0);
    }

    private static PGPKeyRingGenerator generateKeyRingGenerator(String id, char[] pass, int s2kcount)
            throws Exception {
        RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();

        kpg.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), 2048, 12));

        PGPKeyPair rsakp_sign = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, kpg.generateKeyPair(), new Date());
        PGPKeyPair rsakp_enc = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), new Date());

        PGPSignatureSubpacketGenerator signhashgen = new PGPSignatureSubpacketGenerator();

        signhashgen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
        signhashgen.setPreferredSymmetricAlgorithms(false, new int[]{SymmetricKeyAlgorithmTags.AES_256,
                SymmetricKeyAlgorithmTags.AES_192, SymmetricKeyAlgorithmTags.AES_128});
        signhashgen.setPreferredHashAlgorithms(false, new int[]{HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA1,
                HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA224,});
        signhashgen.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

        PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
        enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

        PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
        PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);

        PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc,
                s2kcount)).build(pass);

        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign, id,
                sha1Calc, signhashgen.generate(), null,
                new BcPGPContentSignerBuilder(rsakp_sign.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), pske);

        keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);
        return keyRingGen;
    }

    public static PGPPublicKey readPublicKey(String publicKey) {
        try {
            PGPPublicKeyRing keyRing = getKeyring(publicKey);
            return getEncryptionKey(keyRing);
        } catch (IOException e) {
            return null;
        }
    }

    private static PGPPublicKeyRing getKeyring(String keyBlock) throws IOException {

        Base64Encoder encoder = new Base64Encoder();
        ByteArrayOutputStream data = new ByteArrayOutputStream(2048);
        encoder.decode(keyBlock, data);

        PGPObjectFactory factory = new PGPObjectFactory(data.toByteArray(), new BcKeyFingerprintCalculator());

        Object o = factory.nextObject();
        if (o instanceof PGPPublicKeyRing) {
            return (PGPPublicKeyRing) o;
        }
        throw new IllegalArgumentException("Input text does not contain a PGP Public Key");
    }

    /**
     * Get the first encyption key off the given keyring.
     */
    private static PGPPublicKey getEncryptionKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null)
            return null;

        // iterate over the keys on the ring, look for one
        // which is suitable for encryption.
        Iterator keys = keyRing.getPublicKeys();
        PGPPublicKey key;
        while (keys.hasNext()) {
            key = (PGPPublicKey) keys.next();
            if (key.isEncryptionKey()) {
                return key;
            }
        }
        return null;
    }

}