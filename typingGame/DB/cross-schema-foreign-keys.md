# 跨 schema 外键清单（P4-1 降级记录）

切分 schema 后以下外键无法跨库创建，改为「逻辑引用」：服务写入前由各自的 `support/client` 调目标服务 `/api/v1/internal/**` 校验。

| 子表 | 目标表 | 约束名 | 子 schema | 目标 schema |
| --- | --- | --- | --- | --- |
| `character_stats` | `user_info` | `fk_character_stats_user` | typing_game_game | typing_game_user |
| `custom_dictionaries` | `user_info` | `fk_custom_dictionaries_user` | typing_game_dict | typing_game_user |
| `user_model_params` | `user_info` | `fk_user_model_params_user` | typing_game_game | typing_game_user |
| `user_monster_configs` | `user_info` | `fk_user_monster_configs_user` | typing_game_game | typing_game_user |
| `string_parents` | `user_info` | `fk_string_parents_user` | typing_game_game | typing_game_user |
| `user_game_mode_configs` | `user_info` | `fk_user_game_mode_user` | typing_game_game | typing_game_user |
| `game_char_type_configs` | `user_info` | `fk_game_char_type_configs_user` | typing_game_game | typing_game_user |
| `user_conversation` | `user_info` | `fk_user_conversation_user` | typing_game_chat | typing_game_user |
| `user_conversation` | `user_info` | `fk_user_conversation_target` | typing_game_chat | typing_game_user |
| `chat_attachment` | `user_info` | `fk_chat_attachment_owner` | typing_game_chat | typing_game_user |
| `system_notification` | `user_info` | `fk_system_notification_user` | typing_game_chat | typing_game_user |
| `game_session` | `user_info` | `fk_game_session_user` | typing_game_game | typing_game_user |
| `game_record` | `user_info` | `fk_game_record_user` | typing_game_game | typing_game_user |
| `leaderboard_entry` | `user_info` | `fk_leaderboard_entry_user` | typing_game_game | typing_game_user |
| `game_mode_configs` | `user_info` | `fk_game_mode_configs_create` | typing_game_game | typing_game_user |
| `game_mode_configs` | `user_info` | `fk_game_mode_configs_update` | typing_game_game | typing_game_user |
| `user_model_adapt_logs` | `user_info` | `fk_user_adapt_logs_user` | typing_game_game | typing_game_user |
| `friend_chat_message` | `user_info` | `fk_friend_chat_message_sender` | typing_game_chat | typing_game_user |
| `friend_chat_message` | `user_info` | `fk_friend_chat_message_receiver` | typing_game_chat | typing_game_user |
| `ai_conversation` | `user_info` | `fk_ai_conversation_user` | typing_game_ai | typing_game_user |
| `ai_chat_message` | `user_info` | `fk_ai_chat_message_user` | typing_game_ai | typing_game_user |

合计 21 条。