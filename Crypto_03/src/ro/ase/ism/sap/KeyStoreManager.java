package ro.ase.ism.sap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class KeyStoreManager {
	
	public static void list(String keyStoreFile, String keyStorePass) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		File ksFile = new File(keyStoreFile);
		if(!ksFile.exists()) {
			System.out.println("------------ File is missing --------------");
			throw new FileNotFoundException();
		}
		
		FileInputStream fis = new FileInputStream(ksFile);
		
		KeyStore ks = KeyStore.getInstance("jks");
		ks.load(fis, keyStorePass.toCharArray());
		
		System.out.println(keyStoreFile + " content: ");
		
		Enumeration<String> entries = ks.aliases();
		while(entries.hasMoreElements()) {
			String entry = entries.nextElement();
			System.out.println(entry);	
			
			if(ks.isKeyEntry(entry)) {
				System.out.println(" - PrivateKeyEntry");
			}
			if(ks.isCertificateEntry(entry)) {
				System.out.println(" - trustedCertEntry");
			}		
		}
		fis.close();	
	}
	
	public static KeyStore getKeyStore(String keyStoreFile, String keyStorePass, String type) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		File ksFile = new File(keyStoreFile);
		if(!ksFile.exists()) {
			System.out.println("File not found");
			throw new FileNotFoundException();
		}
		
		FileInputStream fis = new FileInputStream(ksFile);
		
		KeyStore ks = KeyStore.getInstance(type);
		ks.load(fis, keyStorePass.toCharArray());
		fis.close();
		
		return ks;	
	}
	
	public static PublicKey getPublicKey(KeyStore ks, String entryAlias) 
			throws UnexpectedException, KeyStoreException {
		if(ks == null) {
			System.out.println("No Key Store available");
			throw new UnexpectedException("No keystore");
		}
		
		if(ks.containsAlias(entryAlias)) {			
			return ks.getCertificate(entryAlias).getPublicKey();		
		}else {
			return null;
		}	
	}
	
	public static PrivateKey getPrivateKey(KeyStore ks, String entryAlias, String keyPass) 
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, UnexpectedException {
		if(ks == null) {
			System.out.println("No Key Store available");
			throw new UnexpectedException("No keystore");
		}
		
		if(ks.containsAlias(entryAlias)) {			
			return (PrivateKey) ks.getKey(entryAlias, keyPass.toCharArray());	
		}else {
			return null;
		}	
	}
	
	public static PublicKey getCertificateKey(String cerFileName) 
			throws CertificateException, IOException {
		
		File cerFile = new File(cerFileName);
		if(!cerFile.exists()) {
			System.out.println("NO certificate file");
			throw new FileNotFoundException();
		}
		
		FileInputStream fis = new FileInputStream(cerFile);
		
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(fis);
		
		fis.close();
		
		return certificate.getPublicKey();	
	}
	
	
	public static byte[] generateRandomKey(String encAlgorithm, int keySize) 
			throws NoSuchAlgorithmException {
		
		KeyGenerator keyGenertor = KeyGenerator.getInstance(encAlgorithm);
		keyGenertor.init(keySize);
		return keyGenertor.generateKey().getEncoded();
		
	}
	
	public static byte[] encryptRSA(Key key, byte[] input) 
			throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		
		//Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(input);	
	}

	public static byte[] decryptRSA(Key key, byte[] input) 
			throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		
		//Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(input);	
	}
	
	
	public static byte[] generateDigitalSignature(String fileName, PrivateKey privKey) 
			throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
		
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(privKey);
		
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("The file is missing");
			throw new FileNotFoundException();
		}

		FileInputStream fis = new FileInputStream(file);
		
		
		//let'suppose the file is small and we can read it at once
		byte[] fileContent = fis.readAllBytes();
		
		signature.update(fileContent);
		byte[] signValue = signature.sign();
		
		return signValue;
		
	}
	
	public static boolean hasValidSignature(String fileName, PublicKey pubKey, byte[] digitalSignature) 
			throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
		
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify(pubKey);
		
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("The file is missing");
			throw new FileNotFoundException();
		}

		FileInputStream fis = new FileInputStream(file);
		//let'suppose the file is small and we can read it at once
		byte[] fileContent = fis.readAllBytes();
		
		signature.update(fileContent);
		
		fis.close();
		
		return signature.verify(digitalSignature);	
				
	}
	
	public static void storeData(String filename, byte[] values) throws IOException {
		File file = new File(filename);
		if(!file.exists()) {
			file.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(values);
		
		fos.close();
	}
	
	public static byte[] readData(String filename) throws IOException {
		File file = new File(filename);
		if(!file.exists()) {
			System.out.println("The file is not there");
			throw new FileNotFoundException();
		}
		
		FileInputStream fis = new FileInputStream(file);
		byte[] values = fis.readAllBytes();
		
		fis.close();
		
		return values;
	}
	
	
	
	
	
	
	
}
