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
fun UserAgreementScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户协议") },
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
                text = "用户协议",
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

            SectionTitle("一、总则")
            BodyText(
                "欢迎使用 AI 学伴（以下简称\"本应用\"）。本协议是您（以下简称\"用户\"或\"您\"）与我们之间关于使用本应用服务所订立的协议。请您在开始使用本应用前仔细阅读本协议的全部内容。如您不同意本协议的任何条款，请立即停止注册和使用本应用。"
            )

            SectionTitle("二、账户注册与管理")
            SubSectionTitle("2.1 账户注册")
            BulletText("您应当提供真实、准确、完整的注册信息，并在信息变更后及时更新。")
            BulletText("您注册的账户仅限本人使用，不得出借、出租、转让或赠与他人。")
            BulletText("您对使用该账户进行的所有活动和行为承担法律责任。")

            SubSectionTitle("2.2 账户安全")
            BulletText("您应妥善保管账户密码和登录凭证，因账号被盗用或密码泄露导致的损失由您自行承担。")
            BulletText("如发现账户异常，请立即通过客服联系我们。")

            SubSectionTitle("2.3 账户注销")
            BulletText("您可以在\"个人中心\"申请注销账户。注销后，我们将删除您的个人信息，但法律另有规定的除外。")
            BulletText("账户注销不可恢复，请谨慎操作。")

            SectionTitle("三、服务内容")
            BodyText("本应用向您提供的服务包括但不限于：")
            BulletText("AI 智能答疑：通过文字或语音输入提问，由 AI 提供解答。")
            BulletText("拍照解题：拍摄题目照片，通过 OCR 识别并生成答案和解析。")
            BulletText("语音交互：通过语音输入与 AI 进行对话式学习。")
            BulletText("学习报告：生成个人学习数据统计和分析报告（含 PDF 导出）。")
            BulletText("题库测验：提供各学科练习题和测验功能。")
            BulletText("智能 Agent：支持联网搜索、工具调用等增强功能（v2.0 及以上版本）。")
            BulletText("我们保留根据业务发展调整、增减服务内容及功能模块的权利。")

            SectionTitle("四、用户行为规范")
            SubSectionTitle("4.1 合法使用")
            BodyText("您承诺在使用本应用时遵守相关法律法规，不得利用本应用从事任何违法活动，包括但不限于：")
            BulletText("上传、发布或传播违法信息、淫秽色情内容、暴力恐怖内容等。")
            BulletText("侵犯他人知识产权、商业秘密、隐私权等合法权益。")
            BulletText("利用本应用进行考试作弊、代写作业等学术不端行为。")
            BulletText("试图破解、逆向工程、篡改本应用的代码或安全机制。")
            BulletText("恶意刷接口、爬取数据或进行其他可能影响服务稳定性的行为。")

            SubSectionTitle("4.2 内容规范")
            BodyText("您通过本应用提交的问题、答案、评论等内容，您保留所有权，但授予我们在全球范围内免费的、不可撤销的许可，用于服务的提供和优化（不包含向第三方披露个人信息）。我们有权对违反本协议的内容进行删除或屏蔽。")

            SectionTitle("五、AI 服务特别条款")
            BodyText("本应用提供的 AI 答疑功能基于大语言模型技术，您理解并同意：")
            BulletText("AI 回答仅供参考，不构成专业意见。对于关键决策（如考试答案、医疗建议等），请以专业来源为准。")
            BulletText("AI 可能产生不准确或不适用的回答（即\"AI 幻觉\"），我们对此不承担责任，但会持续优化模型。")
            BulletText("AI 对话内容可能会被记录用于模型训练和改进；如果您不希望对话被用于训练，请在设置中关闭此选项。")
            BulletText("AI Agent 模式下，部分提问可能会触发联网搜索，相关信息将从第三方公开网站获取。")

            SectionTitle("六、知识产权")
            BulletText("本应用及其所有内容（包括但不限于软件、设计、图标、文字、图片、音频、视频等）的知识产权归我们或我们的许可方所有。")
            BulletText("未经我们书面同意，您不得以任何方式复制、修改、传播、出售或利用本应用的知识产权。")
            BulletText("您提交的学习内容的知识产权归您所有，但您授予我们在全球范围内免费的许可，用于提供、维护和优化服务。")

            SectionTitle("七、服务费用")
            BodyText(
                "本应用提供免费和付费两种服务模式。付费会员的具体费用、期限和权益以应用内购买页面为准。所有费用一经支付，除法律另有规定外，不予退还。我们有权调整收费标准和政策，调整前将通过应用内通知提前告知。"
            )

            SectionTitle("八、免责声明")
            BulletText("本应用按\"现状\"和\"可用\"的基础提供服务，不提供任何明示或暗示的保证。")
            BulletText("我们不对因不可抗力、网络故障、系统维护等原因导致的服务中断承担责任。")
            BulletText("我们对您因使用或无法使用本应用所产生的任何直接或间接损失不承担责任。")
            BulletText("我们对第三方链接或第三方服务的内容和可用性不承担责任。")

            SectionTitle("九、协议变更与终止")
            BulletText("我们有权根据需要修改本协议。修改后的协议将在应用内公布，如您继续使用本应用，即视为接受修改后的协议。")
            BulletText("如您违反本协议的任何条款，我们有权暂停或终止您的账户，并保留追究法律责任的权利。")
            BulletText("协议终止后，您应立即停止使用本应用，我们有权删除与您账户相关的数据，但法律另有要求的除外。")

            SectionTitle("十、法律适用与争议解决")
            BodyText(
                "本协议的订立、执行和解释及争议的解决均适用中华人民共和国法律。如双方就本协议内容或其执行发生争议，应友好协商解决；协商不成的，任何一方均可提交至应用运营方所在地有管辖权的人民法院诉讼解决。"
            )

            SectionTitle("十一、联系我们")
            BodyText("如您对本协议有任何疑问或建议，请联系我们：")
            BulletText("电子邮件：support@aitutor.app")
            BulletText("客服电话：400-000-0000")

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ===== Preview =====
@Preview(
    name = "用户协议 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "用户协议 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun UserAgreementScreenPreview() {
    AiTutorTheme {
        UserAgreementScreen(onBack = {})
    }
}
