package uk.gov.hmrc.agentclientmanagementfrontend.modules

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.crypto.{Crypted, PlainBytes, PlainText}

import java.nio.charset.StandardCharsets
import java.util.Base64

class CryptoProviderModuleSpec extends PlaySpec {

  private def configuration(fieldLevelEncryptionEnabled: Boolean): Configuration = Configuration(
    ConfigFactory.parseString(s"""fieldLevelEncryption {
                                 |  enable = $fieldLevelEncryptionEnabled
                                 |  key = "td5GaqQ/bDk47dDWzhchchAT03xpFoUy1wb+YOoA/IM="
                                 |}
                                 |""".stripMargin)
  )

  "CryptoProviderModule" should {
    "provide a real crypto instance if field-level encryption is enabled in config" in {
      val x =
        new CryptoProviderModule().aesCryptoInstance(configuration(fieldLevelEncryptionEnabled = true))
      x must not be a[NoCrypto]
    }
    "provide a no-op crypto instance if field-level encryption is disabled in config" in {
      val x =
        new CryptoProviderModule().aesCryptoInstance(configuration(fieldLevelEncryptionEnabled = false))
      x mustBe a[NoCrypto]
    }
  }

  "NoCrypto" should {
    val text = "Not a secret"
    val bytes: Array[Byte] = Array(0x13, 0x37)
    val base64Bytes = new String(Base64.getEncoder.encode(bytes), StandardCharsets.UTF_8)

    "pass through data on encryption" in {
      NoCrypto.encrypt(PlainText(text)).value mustBe text
      NoCrypto.encrypt(PlainBytes(bytes)).value mustBe base64Bytes
    }

    "pass through data on decryption" in {
      NoCrypto.decrypt(Crypted(text)).value mustBe text
      NoCrypto.decryptAsBytes(Crypted(base64Bytes)).value mustBe bytes
    }
  }
}
