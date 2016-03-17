package com.kermekx.smartchatting.pgp;

import com.kermekx.smartchatting.hash.Hasher;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.encoders.Base64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by kermekx on 03/03/2016.
 * s
 */
public class PGPManager {

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
            PGPPublicKeyRing keyRing = getPublicKeyRing(publicKey);
            return getPublicKey(keyRing);
        } catch (IOException e) {
            return null;
        }
    }

    private static PGPPublicKeyRing getPublicKeyRing(String keyBlock) throws IOException {

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

    private static PGPPublicKey getPublicKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null)
            return null;

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

    public static PGPSecretKeyRing readSecreteKeyRing(String secreteKey) {
        try {
            return getSecreteKeyRing(secreteKey);
        } catch (IOException e) {
            //bad key block (corrupted, network error, ...)
            return null;
        }
    }

    public static char[] generateKeyPassword(byte[] password, byte[] pin) {
        byte[] pass = new BigInteger(password).multiply(new BigInteger(pin)).toByteArray();
        return new String(pass).toCharArray();
    }

    /**
     * Used to verify username and pin, to decrypt messages, use readSecreteKeyRing(generateKeyPassword(password, pin))
     * @param privateKey
     * @param password
     * @param pin
     * @return
     */
    public static PGPPrivateKey readPrivateKey(String privateKey, byte[] password, byte[] pin) {
        try {
            PGPSecretKeyRing keyRing = getSecreteKeyRing(privateKey);
            PGPSecretKey secretKey = getSecretKey(keyRing);

            byte[] pass = new BigInteger(password).multiply(new BigInteger(pin)).toByteArray();
            PBESecretKeyDecryptor pskd = (new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())).build(new String(pass).toCharArray());

            try {
                return secretKey.extractPrivateKey(pskd);
            } catch (PGPException e) {
                //wrong password(s)
                return null;
            }
        } catch (IOException e) {
            //bad key block (corrupted, network error, ...)
            return null;
        }
    }

    private static PGPSecretKeyRing getSecreteKeyRing(String keyBlock) throws IOException {

        Base64Encoder encoder = new Base64Encoder();
        ByteArrayOutputStream data = new ByteArrayOutputStream(2048);
        encoder.decode(keyBlock, data);

        PGPObjectFactory factory = new PGPObjectFactory(data.toByteArray(), new BcKeyFingerprintCalculator());

        Object o = factory.nextObject();
        if (o instanceof PGPSecretKeyRing) {
            return (PGPSecretKeyRing) o;
        }
        throw new IllegalArgumentException("Input text does not contain a PGP secret Key");
    }

    private static PGPSecretKey getSecretKey(PGPSecretKeyRing keyRing) {
        if (keyRing == null)
            return null;

        Iterator keys = keyRing.getSecretKeys();
        PGPSecretKey key;
        while (keys.hasNext()) {
            key = (PGPSecretKey) keys.next();
            return key;
        }
        return null;
    }

    public static boolean encode(String keyBlock, byte[] message, ByteArrayOutputStream output) {
        try {
            PGPPublicKey key = readPublicKey(keyBlock);

            PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
            ByteArrayOutputStream ldOut = new ByteArrayOutputStream();
            OutputStream pOut = lData.open(ldOut, PGPLiteralDataGenerator.UTF8, PGPLiteralData.CONSOLE, message.length,
                    new Date());

            pOut.write(message);
            pOut.close();

            byte[] data = ldOut.toByteArray();

            PGPDataEncryptorBuilder encryptBuilder = new BcPGPDataEncryptorBuilder(PGPEncryptedData.AES_256);

            PGPEncryptedDataGenerator encryptGen = new PGPEncryptedDataGenerator(encryptBuilder);
            encryptGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(key));

            ByteArrayOutputStream edOut = new ByteArrayOutputStream();
            OutputStream encryptedOut = encryptGen.open(edOut, new byte[data.length + 128]);

            PGPCompressedDataGenerator compressor = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
            OutputStream compressedOut = compressor.open(encryptedOut);

            compressedOut.write(data);
            compressedOut.close();
            encryptedOut.close();

            byte[] bytes = edOut.toByteArray();

            Base64Encoder encoder = new Base64Encoder();
            encoder.encode(bytes, 0, bytes.length, output);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean decode(PGPSecretKeyRing secretKeyRing, char[] password, String message, ByteArrayOutputStream output) {

        PGPPublicKeyEncryptedData encryptedData;

        try {
            Base64Encoder decoder = new Base64Encoder();
            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            decoder.decode(message, decoded);

            InputStream is = PGPUtil.getDecoderStream(new ByteArrayInputStream(decoded.toByteArray()));

            PGPObjectFactory pgpF = new BcPGPObjectFactory(is);
            Object o = pgpF.nextObject();
            PGPEncryptedDataList enc = (o instanceof PGPEncryptedDataList) ? (PGPEncryptedDataList) o
                    : (PGPEncryptedDataList) pgpF.nextObject();
            encryptedData = null;
            PGPPrivateKey privateKey = null;
            for (Iterator<PGPPublicKeyEncryptedData> iterator = enc.getEncryptedDataObjects(); iterator.hasNext(); ) {
                encryptedData = iterator.next();
                PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(
                        new BcPGPDigestCalculatorProvider()).build(password);
                PGPSecretKey secretKey = secretKeyRing.getSecretKey(encryptedData.getKeyID());
                if (secretKey != null) {
                    privateKey = secretKey.extractPrivateKey(decryptor);
                    continue;
                }
            }
            if (privateKey == null) {
                throw new IllegalArgumentException("Unable to find secret key to decrypt the message : " + new String(message));
            }

            PGPObjectFactory plainFact = new BcPGPObjectFactory(
                    encryptedData.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey)));
            Object compressed = plainFact.nextObject();
            if (compressed instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) compressed;
                PGPObjectFactory pgpFact = new BcPGPObjectFactory(cData.getDataStream());
                compressed = pgpFact.nextObject();
            }

            if (compressed instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) compressed;
                InputStream unc = ld.getInputStream();
                int ch;
                while ((ch = unc.read()) >= 0) {
                    output.write(ch);
                }
            } else if (compressed instanceof PGPOnePassSignatureList) {
                throw new PGPException("encrypted message contains a signed message - not literal data.");
            } else {
                throw new PGPException("message is not a simple encrypted file - type unknown.");
            }

            if (encryptedData.isIntegrityProtected()) {
                if (!encryptedData.verify()) {
                    System.err.println("message failed integrity check");
                }
            } else {
                System.err.println("no message integrity check");
            }
            return true;
        } catch (PGPException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println();
            e.printStackTrace();
            return false;
        }
    }
}
