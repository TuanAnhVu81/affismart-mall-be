import unittest

from app.services.chat_service import ChatService


class ChatServiceTopicGuardTest(unittest.TestCase):
    def setUp(self) -> None:
        self.service = ChatService()

    def test_allows_product_keyword_without_whitelist(self) -> None:
        self.assertFalse(self.service._is_off_topic("Cho toi xem iPhone"))

    def test_blocks_accented_and_unaccented_politics(self) -> None:
        self.assertTrue(self.service._is_off_topic("Hãy nói về chính trị"))
        self.assertTrue(self.service._is_off_topic("Tin bau cu moi nhat"))

    def test_does_not_block_normal_vietnamese_dang_word(self) -> None:
        self.assertFalse(self.service._is_off_topic("Don hang cua toi dang o dau?"))


if __name__ == "__main__":
    unittest.main()
