# Privacy Policy / 隐私政策

**Last updated: 2026-05-16**

This privacy policy describes how AI Tutor / AI 学伴 ("we", "our", or "the App")
collects, uses, stores, and shares your personal information when you use our mobile application.

**Applicable to version:** 1.0+

**Package name:** `com.aitutor.app`

---

## 1. Information We Collect

### 1.1 Information You Provide

| Data Type | Details | Purpose |
|---|---|---|
| Account Credentials | Email address and/or phone number, password (hashed) | User account registration and authentication |
| Profile Information | Nickname, avatar, grade level, preferred subjects | Personalization of learning experience |
| Learning Content | Text questions, voice recordings, photo submissions, quiz answers, AI conversation history | Core service delivery (AI tutoring, OCR solving, voice interaction) |
| Subscription Data | Purchase receipts, subscription tier | Managing paid memberships |
| Feedback | Customer support inquiries, bug reports | Service improvement and issue resolution |

### 1.2 Information Collected Automatically

| Data Type | Details | Purpose |
|---|---|---|
| Device Info | Device model, OS version, Android ID, IP address, network type | Service compatibility, analytics, security |
| Usage Logs | Feature usage frequency, session duration, click events | Product optimization, personalization |
| Performance Data | Crash logs, API latency, error reports | Stability monitoring, bug fixing |

### 1.3 Permissions

The App requests the following Android permissions. Each is used **only when you actively use the corresponding feature**:

| Permission | When Used | Data Collected On-Device Only? |
|---|---|---|
| `CAMERA` | Taking photos of homework/questions for OCR solving | Yes — images processed locally first, then sent to AI API for recognition |
| `RECORD_AUDIO` | Voice input for AI questions | Yes — audio sent to speech-to-text backend |
| `INTERNET` | API communication with AI backend | N/A — core network essential |
| `ACCESS_NETWORK_STATE` | Checking network availability | N/A |
| `POST_NOTIFICATIONS` | Learning reminders, report alerts | N/A |
| `READ_EXTERNAL_STORAGE` (maxSdk=32) | Selecting images from gallery | Yes |

### 1.4 Third-Party SDKs

| SDK | Purpose | Data Shared | Privacy Link |
|---|---|---|---|
| CameraX (Google) | Camera preview and photo capture | Camera frame data (on-device only) | [CameraX](https://developer.android.com/training/camerax) |
| ML Kit (Google) | OCR text recognition, translation | Image text content | [ML Kit Terms](https://developers.google.com/ml-kit/terms) |
| Retrofit / OkHttp | Network API communication | API request parameters (question content, user ID) | [OkHttp](https://square.github.io/okhttp/) |
| Firebase Crashlytics | Crash reporting | Crash stack traces, device info | [Firebase Privacy](https://firebase.google.com/support/privacy) |
| In-app Billing (Google Play) | Subscription management | Purchase tokens | [Google Play Billing](https://payments.developers.google.com/terms) |

> **Note:** If third-party login (WeChat, Google Sign-In) or push notification SDKs are integrated in future versions, this section will be updated accordingly.

---

## 2. How We Use Your Information

- **Service delivery**: AI question answering, photo solving, voice interaction, learning reports
- **Personalization**: Tailored learning content recommendations based on your study history
- **Product improvement**: Aggregated analytics to improve features and AI response quality
- **Communication**: Send learning reminders, subscription notifications, and service updates
- **Security**: Fraud detection, abuse prevention, account safety monitoring
- **Legal compliance**: Fulfill legal obligations (data retention, law enforcement requests)

We **do not** use your learning content for purposes other than providing and improving the AI tutoring service.
Your personal conversations with the AI are not sold, rented, or shared with third-party advertisers.

---

## 3. AI & Data Processing

### 3.1 AI Backend

- Your questions, photos, and voice inputs are sent to our backend server for AI processing.
- Conversations may be logged for quality improvement. You can disable this in the App settings.
- When using AI Agent mode (v2.0+), queries may trigger internet search via third-party public websites.

### 3.2 Data Safety (Google Play Data Safety Section)

| Category | Collected | Shared | Ephemeral | Required for App |
|---|---|---|---|---|
| Location | No | No | N/A | No |
| Personal Info (email, name, user ID) | Yes | No | No | Yes |
| Financial (subscription) | Yes (via Google Play) | No | No | No |
| Messages (questions, AI conversations) | Yes | No | No | Yes |
| Photos/Videos | Yes (camera/gallery) | No | Yes (processed then stored) | Yes (photo solving) |
| Audio/Voice | Yes (voice input) | No | Yes | No |
| App Activity (usage stats) | Yes | No | No | No |
| App Diagnostics (crash logs) | Yes | Yes (Firebase) | No | No |
| Device ID | Yes | No | No | No |

---

## 4. Data Storage & Security

| Measure | Implementation |
|---|---|
| Encryption in transit | TLS 1.3/1.2 for all API communications |
| Encryption at rest | Sensitive user data encrypted at the server level |
| Access control | Only authorized personnel access user data; strict audit logging |
| Data retention | Account data retained until account deletion; learning content retained for service continuity (deleted upon account deletion or within 90 days of service termination) |
| Local data | Some learning data cached locally; users can clear cache in App settings |

---

## 5. Your Rights & Choices

| Right | How to Exercise |
|---|---|
| Access | View your profile and learning history in the App |
| Correction | Edit profile info in the App settings |
| Deletion | Delete your account in "Profile > Account > Delete Account" (data removed within 30 days) |
| Withdraw Consent | Disable camera/microphone permissions in device settings |
| Opt-out of AI training | Toggle "Improve AI with conversations" in App settings |
| Data Portability | Contact us to request a copy of your data |
| Objection | You may object to data processing by contacting us |

To exercise any of these rights, contact: **privacy@aitutor.app**

---

## 6. Children's Privacy

The App is designed for K-12 students. We take children's privacy seriously:

- **Age threshold**: We follow COPPA (US, under 13), GDPR-K / UK (under 13), and China's Personal Information Protection Law (under 14).
- **Parental consent required**: If the user is under the applicable age threshold, a parent or guardian must consent to data collection.
- **What we collect from children**: Minimal necessary data — learning content and basic profile (nickname, grade level). We do not collect precise location, contacts, or browsing history.
- **Parental rights**: Parents may review, delete, or refuse further collection of their child's data by contacting us.
- **No behavioral advertising**: We do not serve behavioral ads to children.
- **Verification**: We may request the parent's email for verification before activating an account for a child user.

> **If you are a parent or guardian** and believe your child has provided personal information without your consent, please contact us immediately at **privacy@aitutor.app** and we will delete that data.

---

## 7. Third-Party Sharing & Data Transfers

- We do **not** sell personal information to third parties.
- We do **not** share data for cross-context behavioral advertising.
- Service providers (cloud hosting, AI model APIs, analytics) are contractually bound to process data only on our instructions and to implement equivalent security measures.
- If we are required to disclose data by law (court order, government request), we will notify you where legally permitted.

### International Transfers

Our servers are located in [Singapore / US / region]. Data may be transferred to and processed in jurisdictions outside your country of residence. We ensure appropriate safeguards (Standard Contractual Clauses) are in place for such transfers.

---

## 8. Data Breach Notification

In the event of a data breach that compromises your personal information, we will:
1. Notify affected users within 72 hours of discovery.
2. Describe the nature and scope of the breach.
3. Recommend steps you can take to protect yourself.
4. Report to relevant regulatory authorities as required by applicable law.

---

## 9. Changes to This Policy

We may update this privacy policy from time to time. Material changes will be notified via:
- In-app notification or popup
- Updated "Last updated" date at the top of this document
- Email notification (if you have provided your email)

We encourage you to review this policy periodically.

---

## 10. Contact Us

| Purpose | Contact |
|---|---|
| Privacy questions | privacy@aitutor.app |
| Technical support | support@aitutor.app |
| Data deletion requests | privacy@aitutor.app |
| Parental concerns | privacy@aitutor.app |

**Mailing address:** [To be filled — company address]

> **⚠️ Note to developers:** Replace the contact email addresses above with real operational addresses before publishing. The domain `aitutor.app` should either be registered and configured with email service, or use a real email address.

---

## 11. Hosting This Policy

Google Play requires the privacy policy to be accessible via a **public URL**. Options:

### Option A: GitHub Pages (Recommended)
1. Create a repository hosted on GitHub (e.g., `your-org/aitutor-privacy`)
2. Place the `web/privacy_policy.html` in the `docs/` folder or root
3. Enable GitHub Pages in repository Settings
4. URL will be: `https://your-org.github.io/aitutor-privacy/privacy_policy.html`

### Option B: Google Drive
1. Upload `web/privacy_policy.html` to Google Drive
2. Share as "Anyone with the link can view"
3. Copy the share URL

### Option C: Custom Domain
- If the App has a website (e.g., `https://aitutor.app/privacy`), host the HTML there
- This is the most professional option

**After hosting, enter the URL in:**
Google Play Console > Select App > Store presence > Store settings > Privacy Policy

---

## Appendix A: Compliance Checklist

- [ ] Privacy policy hosted at a public URL
- [ ] Privacy policy URL entered in Google Play Console
- [ ] Google Play Data Safety form completed (matching this policy)
- [ ] Age rating questionnaire completed in Play Console
- [ ] COPPA / children's privacy safeguards implemented
- [ ] Parental consent flow for under-age users (if applicable)
- [ ] Contact information verified and active
- [ ] Third-party SDK data collection reviewed
- [ ] AI conversation opt-out toggle available in settings
- [ ] Account deletion mechanism implemented
