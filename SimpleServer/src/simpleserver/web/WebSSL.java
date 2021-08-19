/*
 * Copyright (c) 2021, mirafun
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package simpleserver.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import simpleserver.log.Log;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import static simpleserver.web.Utils.days;

public class WebSSL {
    
    public Conf conf;
    private final File WEB_SSL_DIR;
    private final File TIME_FILE;
    private final File USER_KEY_FILE;
    public final File DOMAIN_KEY_FILE;
    private final File DOMAIN_CSR_FILE;
    public final File DOMAIN_CHAIN_FILE;
    
    public WebSSL(Conf conf) {
        this.conf = conf;
        
        WEB_SSL_DIR = conf.path.dirs(".webssl");
        TIME_FILE = new File(WEB_SSL_DIR, "time.data");
        USER_KEY_FILE = new File(WEB_SSL_DIR, "user.key");
        DOMAIN_KEY_FILE = new File(WEB_SSL_DIR, "domain.key");
        DOMAIN_CSR_FILE = new File(WEB_SSL_DIR, "domain.csr");
        DOMAIN_CHAIN_FILE = new File(WEB_SSL_DIR, "domain-chain.crt");
    }
    
    private volatile String file, chall;
    Properties p = new Properties();
    
    public void init() throws Exception {
        if(TIME_FILE.exists()) {
            p.loadFromXML(new BufferedInputStream(new FileInputStream(TIME_FILE)));
        }
        else {
            p.setProperty("time", "0");
            p.setProperty("last", "0");

        }
    }
    public void update() {
        if(conf.sslLetsEncrypt == null) return;
        try {
//            String domain = conf.websitePath;
//            int i = domain.indexOf("://");
//            if(i != -1) domain = domain.substring(i+3);
            Log.log("Initializing update of ssl: " + conf.sslLetsEncrypt.domains);
            fetchCertificate(conf.sslLetsEncrypt.domains);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            setLast();
            saveProps();
        }
    }
    public void setHttpChallenge(String file, String chall) {
        Log.log("Http Challenge " + file + " " + chall);
        this.file = file;
        this.chall = chall;
    }
    public String getHttpChallengeFile() { 
        return file;
    }
    public String getHttpChallengeContent() { 
        return chall;
    }
    public void setTime() {
        long no = System.currentTimeMillis();
        p.setProperty("time", ""+no);
    }
    public void setLast() {
        long no = System.currentTimeMillis();
        p.setProperty("time", ""+no);
    }
    public String getAccountURL() {
        return p.getProperty("account");
    }
    public void setAccountURL(String loc) {
        p.setProperty("account", loc);
    }
    public void saveProps() {
        try {
            p.storeToXML(new FileOutputStream(TIME_FILE), "", StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public void updateIfNeeded() throws Exception {
        if(DOMAIN_KEY_FILE.exists() && DOMAIN_CHAIN_FILE.exists()) {
            long time = Long.parseLong((String)p.getOrDefault("time", "0"));
            long last = Long.parseLong((String)p.getOrDefault("last", "0"));
            
            long no = System.currentTimeMillis();
            long da = days(no-time);
            Log.log("check ssl: " + da);
            if(da > 60) {
                if(days(no-last) >= 1) {
                    update();
                }
            }
        }
        else {
            update();
        }
    }
    
    
    
    private enum ChallengeType {HTTP, DNS}
    // File name of the User Key Pair
    

    //Challenge type to be used
    private static final ChallengeType CHALLENGE_TYPE = ChallengeType.HTTP;

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;
    
    // https://acme-staging-v02.api.letsencrypt.org/directory
    // https://acme-v02.api.letsencrypt.org/directory
    
    public void createAccount() throws IOException, AcmeException {
        String str = getAccountURL();
        if(str != null) {
            Log.log("Account already created: ", str);
            return;
        }
        KeyPair userKeyPair = loadOrCreateUserKeyPair();
        Session session = new Session(conf.debug?"acme://letsencrypt.org/staging":"acme://letsencrypt.org");
        Account acct = findOrRegisterAccount(session, userKeyPair);
    }
    
    public void fetchCertificate(Collection<String> domains) throws IOException, AcmeException {
        String str = getAccountURL();
        if(str == null) {
            Log.log("No account.");
            return;
        }
        URL loc = new URL(str);
        
        // Load the user key file. If there is no key file, create a new one.
        KeyPair userKeyPair = loadOrCreateUserKeyPair();

        // Create a session for Let's Encrypt.
        // Use "acme://letsencrypt.org" for production server
        Session session = new Session(conf.debug?"acme://letsencrypt.org/staging":"acme://letsencrypt.org");

        // Get the Account.
        // If there is no account yet, create a new one.
//        Account acct = findOrRegisterAccount(session, userKeyPair);
        Account acct = login(loc, session, userKeyPair);

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateDomainKeyPair();

        // Order the certificate
        Order order = acct.newOrder().domains(domains).create();

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth);
        }

        // Generate a CSR for all of the domains, and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomains(domains);
        csrb.sign(domainKeyPair);

        // Write the CSR to a file, for later use.
        try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
            csrb.write(out);
        }

        // Order the certificate
        order.execute(csrb.getEncoded());

        // Wait for the order to complete
        int attempts = 10;
        Utils.restInterruptibly(3000L);
        while (order.getStatus() != Status.VALID && attempts-- > 0) {
            // Did the order fail?
            if (order.getStatus() == Status.INVALID) {
                Log.log("Order has failed, reason: {}", order.getError());
                throw new AcmeException("Order failed... Giving up.");
            }

            // Wait for a few seconds
            Utils.restInterruptibly(3000L);
            // Then update the status
            order.update();
        }

        // Get the certificate
        Certificate certificate = order.getCertificate();
        
        if(certificate == null) {
            throw new AcmeException("Certificate null.");
        }

        Log.log("Success! The certificate for domains {} has been generated!", domains);
        Log.log("Certificate URL: {}", certificate.getLocation());

        // Write a combined file containing the certificate and chain.
        try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
            certificate.writeCertificate(fw);
        }
        
        setTime();

        // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
        // DOMAIN_CHAIN_FILE for the requested domains.
    }
    
    private KeyPair loadOrCreateUserKeyPair() throws IOException {
        if (USER_KEY_FILE.exists()) {
            // If there is a key file, read it
            try (FileReader fr = new FileReader(USER_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }

        } else {
            // If there is none, create a new key pair and save it
            KeyPair userKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(USER_KEY_FILE)) {
                KeyPairUtils.writeKeyPair(userKeyPair, fw);
            }
            return userKeyPair;
        }
    }
    
    private KeyPair loadOrCreateDomainKeyPair() throws IOException {
        if (DOMAIN_KEY_FILE.exists()) {
            try (FileReader fr = new FileReader(DOMAIN_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(DOMAIN_KEY_FILE)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }
            return domainKeyPair;
        }
    }
    
    private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {
        // Ask the user to accept the TOS, if server provides us with a link.
        URI tos = session.getMetadata().getTermsOfService();
        if (tos != null) {
//            acceptAgreement(tos);
            Log.log("tos ", tos);
        }

        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(accountKey)
                .create(session);
        
        URL loc = account.getLocation();
        String url = loc.toExternalForm();
        Log.log("Registered a new user, URL: {}", url);
        setAccountURL(url);
        
        return account;
    }
    public Account login(URL loc, Session session, KeyPair accountKey) {
        return session.login(loc, accountKey).getAccount();
    }
    
    private void authorize(Authorization auth) throws AcmeException {
        Log.log("Authorization for domain {}", auth.getIdentifier().getDomain());

        // The authorization is already valid. No need to process a challenge.
        if (auth.getStatus() == Status.VALID) {
            return;
        }

        // Find the desired challenge and prepare it.
        Challenge challenge = null;
        switch (CHALLENGE_TYPE) {
            case HTTP:
                challenge = httpChallenge(auth);
                break;

            case DNS:
//                challenge = dnsChallenge(auth);
                throw new UnsupportedOperationException();
//                break;
        }

        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }

        // If the challenge is already verified, there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            return;
        }

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
        Utils.restInterruptibly(3000L);
        int attempts = 10;
        while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
            // Did the authorization fail?
            if (challenge.getStatus() == Status.INVALID) {
                Log.log("Challenge has failed, reason: {}", challenge.getError());
                throw new AcmeException("Challenge failed... Giving up.");
            }

            // Wait for a few seconds
            Utils.restInterruptibly(3000L);

            // Then update the status
            challenge.update();
        }
        

        // All reattempts are used up and there is still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain "
                    + auth.getIdentifier().getDomain() + ", ... Giving up.");
        }

        Log.log("Challenge has been completed. Remember to remove the validation resource.");
        completeChallenge("Challenge has been completed.\nYou can remove the resource again now.");
    }
    
    public Challenge httpChallenge(Authorization auth) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
        if (challenge == null) {
            throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
        }

        // Output the challenge, wait for acknowledge...
//        LOG.info("Please create a file in your web server's base directory.");
//        LOG.info("It must be reachable at: http://{}/.well-known/acme-challenge/{}",
//                auth.getIdentifier().getDomain(), challenge.getToken());
//        LOG.info("File name: {}", challenge.getToken());
//        LOG.info("Content: {}", challenge.getAuthorization());
//        LOG.info("The file must not contain any leading or trailing whitespaces or line breaks!");
//        LOG.info("If you're ready, dismiss the dialog...");
//
//        StringBuilder message = new StringBuilder();
//        message.append("Please create a file in your web server's base directory.\n\n");
//        message.append("http://")
//                .append(auth.getIdentifier().getDomain())
//                .append("/.well-known/acme-challenge/")
//                .append(challenge.getToken())
//                .append("\n\n");
//        message.append("Content:\n\n");
//        message.append(challenge.getAuthorization());
//        acceptChallenge(message.toString());

        setHttpChallenge( challenge.getToken(), challenge.getAuthorization());
       
        return challenge;
    }
    public void completeChallenge(String message) throws AcmeException {
        setHttpChallenge(null, null);
    }
}
