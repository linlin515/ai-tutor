package com.aitutor.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私政策") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "最后更新日期：2026 年 5 月 16 日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("一、引言")
            BodyText(
                "AI 学伴（以下简称\"本应用\"或\"我们\"）重视用户的隐私保护。本隐私政策说明了我们在您使用本应用时如何收集、使用、存储和披露您的个人信息。请在使用本应用前仔细阅读本政策。"
            )

            SectionTitle("二、我们收集的信息")
            BodyText("为了向您提供更好的服务，我们在您使用本应用时可能收集以下类型的信息：")

            SubSectionTitle("2.1 您主动提供的信息")
            BulletText("注册信息：当您注册账户时，我们可能收集您的手机号码、电子邮箱地址或第三方账号信息（如微信、QQ）。")
            BulletText("个人资料：您可以选择填写昵称、头像、年级、学科偏好等信息，以帮助我们为您提供个性化学习推荐。")
            BulletText("学习内容：您提交的题目照片、语音输入、文字提问、作答记录等学习相关内容。")
            BulletText("反馈信息：您通过客服或反馈功能提交的意见和建议。")

            SubSectionTitle("2.2 我们自动收集的信息")
            BulletText("设备信息：设备型号、操作系统版本、唯一设备标识符（如 Android ID）、IP 地址、网络类型。")
            BulletText("使用日志：您使用本应用各功能的时间、频率、停留时长、点击行为等操作日志。")
            BulletText("性能数据：应用崩溃日志、页面加载速度、API 响应时间等性能监控数据。")

            SubSectionTitle("2.3 相机与媒体权限")
            BulletText("拍照权限：用于拍照上传题目、扫描文字进行 OCR 识别。我们仅在您主动使用拍照功能时调用相机。")
            BulletText("相册读取：用于从相册选择图片上传。我们不会未经授权读取您的相册内容。")
            BulletText("麦克风权限：用于语音输入题目和语音对话功能。我们仅在您主动使用语音输入时录制音频。")

            SectionTitle("三、信息的使用目的")
            BodyText("我们收集的信息将用于以下目的：")
            BulletText("提供和维持本应用的核心功能，包括 AI 答疑、拍照解题、语音交互、学习报告生成等。")
            BulletText("个性化推荐：根据您的学习行为和偏好推荐相应的学习内容和题目。")
            BulletText("服务优化：分析使用数据以改进产品功能、优化用户体验和提升响应速度。")
            BulletText("客户支持：回复您的咨询、处理您反馈的问题。")
            BulletText("安全保障：检测和防范欺诈、滥用及其他非法活动。")

            SectionTitle("四、第三方 SDK 披露")
            BodyText("本应用集成了以下第三方 SDK，它们可能会收集和使用您的设备信息：")

            SubSectionTitle("4.1 CameraX（Google）")
            BulletText("用途：相机预览和拍照功能，用于 OCR 文字识别和题目拍摄。")
            BulletText("收集信息：相机画面数据，仅在本地处理，不发送至远程服务器。")
            BulletText("隐私政策：https://developer.android.com/training/camerax")

            SubSectionTitle("4.2 ML Kit（Google）")
            BulletText("用途：文字识别（OCR）、文本翻译、语言检测。")
            BulletText("收集信息：图片中的文字内容以进行识别，部分处理在本地完成。")
            BulletText("隐私政策：https://developers.google.com/ml-kit/terms")

            SubSectionTitle("4.3 Retrofit / OkHttp")
            BulletText("用途：网络请求框架，用于与我们的后端 API 通信。")
            BulletText("收集信息：传递 API 请求参数和响应数据，包括您的提问内容和 AI 回复。")

            SubSectionTitle("4.4 Firebase Crashlytics")
            BulletText("用途：应用崩溃监控和性能分析。")
            BulletText("收集信息：崩溃堆栈信息、设备型号、操作系统版本。")
            BulletText("隐私政策：https://firebase.google.com/support/privacy")

            SubSectionTitle("4.5 可能集成的其他 SDK")
            BulletText("第三方登录 SDK（如微信登录、QQ 登录）：用于便捷登录。")
            BulletText("推送 SDK：用于发送学习提醒和通知。")

            SectionTitle("五、信息存储与安全")
            BodyText("我们采取符合行业标准的安全措施保护您的个人信息：")
            BulletText("数据加密：在传输过程中使用 TLS/SSL 协议加密；敏感数据在服务端加密存储。")
            BulletText("访问控制：仅限经授权的人员访问用户数据，并实施严格的访问审计。")
            BulletText("数据保留：我们仅在实现本政策所述目的所必需的期限内保留您的个人信息，除非法律另有要求。")
            BulletText("本地数据：部分学习数据存储在您的设备本地，您可以在设置中清除缓存。")

            SectionTitle("六、用户权利")
            BodyText("根据适用的数据保护法律，您享有以下权利：")
            BulletText("访问权：您有权查询我们持有的您的个人信息。")
            BulletText("更正权：如您的个人信息不准确，您有权要求更正。")
            BulletText("删除权：在特定情况下，您有权要求删除您的个人信息。您也可以通过注销账户来行使此权利。")
            BulletText("撤回同意：您可以在设备设置中关闭相机、麦克风等权限以撤回同意。")
            BulletText("注销账户：您可以在\"个人中心\"中申请注销账户，我们将在核实后 7 个工作日内完成注销。")

            SectionTitle("七、儿童隐私保护")
            BodyText(
                "本应用主要面向学生用户。如您是未满 14 周岁的未成年人，请在监护人的陪同下阅读本政策，并在征得监护人同意后使用本应用。我们不会在监护人未同意的情况下故意收集儿童的个人信息。"
            )

            SectionTitle("八、隐私政策的更新")
            BodyText(
                "我们可能会不时更新本隐私政策。更新后的政策将在应用内公布，并标注最后更新日期。重大变更我们将通过应用内通知或弹窗方式告知您。请您定期查看本政策以了解最新的隐私保护措施。"
            )

            SectionTitle("九、联系我们")
            BodyText(
                "如您对本隐私政策有任何疑问、意见或投诉，请通过以下方式联系我们："
            )
            BulletText("电子邮件：privacy@aitutor.app")
            BulletText("客服电话：400-000-0000")

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
internal fun SubSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
internal fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
internal fun BulletText(text: String) {
    Text(
        text = "  ·  $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

// ===== Preview =====
@Preview(
    name = "隐私政策 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "隐私政策 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PrivacyPolicyScreenPreview() {
    AiTutorTheme {
        PrivacyPolicyScreen(onBack = {})
    }
}
