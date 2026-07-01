package com.typinggame.service.impl;

import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionaryWord;
import com.typinggame.entity.game.StringParent;
import com.typinggame.enums.GameMode;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.DictionaryWordRepository;
import com.typinggame.repository.StringParentRepository;
import com.typinggame.repository.SystemDictionaryWordRepository;
import com.typinggame.repository.GameCharTypeConfigRepository;
import com.typinggame.repository.GameModeConfigRepository;
import com.typinggame.service.WordSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 词源服务实现：无尽适应用字符统计+父本；词源界定用自定义/系统词库。
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WordSourceServiceImpl implements WordSourceService {

    private static final int WEAK_CHARS_LIMIT = 20;
    private static final List<Character> DEFAULT_CHAR_POOL = new ArrayList<>();
    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct,operator,chinese,special";

    static {
        for (char c = 'a'; c <= 'z'; c++) DEFAULT_CHAR_POOL.add(c);
        for (char c = '0'; c <= '9'; c++) DEFAULT_CHAR_POOL.add(c);
    }

    private final CharacterStatsRepository characterStatsRepository;
    private final StringParentRepository stringParentRepository;
    private final DictionaryWordRepository dictionaryWordRepository;
    private final SystemDictionaryWordRepository systemDictionaryWordRepository;
    private final GameModeConfigRepository gameModeConfigRepository;
    private final GameCharTypeConfigRepository gameCharTypeConfigRepository;

    @Override
    public List<String> getCandidateWords(Integer userId, String gameMode, int limit,
                                         List<Integer> customDictIds, List<Integer> systemDictIds) {
        if (GameMode.WORD_SOURCE.name().equals(gameMode)) {
            return getWordsFromDictionaries(userId, gameMode, customDictIds, systemDictIds, limit);
        }
        if (GameMode.LIMIT_TIME.name().equals(gameMode)) {
            boolean hasDictIds = (customDictIds != null && !customDictIds.isEmpty())
                    || (systemDictIds != null && !systemDictIds.isEmpty());
            if (hasDictIds) {
                return getWordsFromDictionaries(userId, gameMode, customDictIds, systemDictIds, limit);
            }
        }
        return getWordsForEndlessAdapt(userId, gameMode, limit);
    }

    private List<String> getWordsForEndlessAdapt(Integer userId, String gameMode, int limit) {
        List<String> out = new ArrayList<>();
        List<String> parentStrings = getLatestParentStrings(userId);
        out.addAll(parentStrings);

        GameMode modeEnum;
        try {
            modeEnum = gameMode != null && !gameMode.isEmpty()
                    ? GameMode.valueOf(gameMode)
                    : GameMode.ENDLESS_ADAPT;
        } catch (IllegalArgumentException e) {
            modeEnum = GameMode.ENDLESS_ADAPT;
        }
        List<Character> pool = buildCharPool(userId, modeEnum);
        if (pool.isEmpty()) pool.addAll(DEFAULT_CHAR_POOL);
        Random rnd = new Random();
        int need = Math.max(0, limit - out.size());
        for (int i = 0; i < need; i++) {
            int len = 2 + rnd.nextInt(4);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < len; j++) {
                sb.append(pool.get(rnd.nextInt(pool.size())));
            }
            out.add(sb.toString());
        }
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    /**
     * 词源界定：从用户选择的词库选词，优先「总练习次数少」或「错误次数多」的词。
     */
    private List<String> getWordsFromDictionaries(Integer userId, String gameMode,
                                                  List<Integer> customDictIds,
                                                  List<Integer> systemDictIds, int limit) {
        List<String> words = new ArrayList<>();
        if (customDictIds != null && !customDictIds.isEmpty()) {
            List<DictionaryWord> list = dictionaryWordRepository.findByDictIdIn(customDictIds);
            // 优先总练习次数少、其次错误次数多
            list.sort(Comparator
                    .comparing(DictionaryWord::getTotalPracticeCount, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Comparator.comparing(DictionaryWord::getErrorPracticeCount, Comparator.nullsLast(Comparator.reverseOrder()))));
            for (DictionaryWord w : list) {
                if (w.getWord() != null && !w.getWord().trim().isEmpty()) {
                    words.add(w.getWord().trim());
                }
            }
        }
        if (systemDictIds != null && !systemDictIds.isEmpty()) {
            List<SystemDictionaryWord> list = systemDictionaryWordRepository.findByDictIdIn(systemDictIds);
            // 按「总练习次数少优先，其次错误次数多」排序
            list.sort((a, b) -> {
                int ta = a.getTotal_count() != null ? a.getTotal_count() : 0;
                int tb = b.getTotal_count() != null ? b.getTotal_count() : 0;
                if (ta != tb) {
                    return Integer.compare(ta, tb);
                }
                int ea = a.getError_count() != null ? a.getError_count() : 0;
                int eb = b.getError_count() != null ? b.getError_count() : 0;
                // 错误次数多的排前面
                return Integer.compare(eb, ea);
            });
            for (SystemDictionaryWord w : list) {
                if (w.getWord() != null && !w.getWord().trim().isEmpty()) {
                    words.add(w.getWord().trim());
                }
            }
        }
        // 按当前模式下的字符类型配置过滤词（只保留全部字符都在允许字符集内的词）
        Set<Character> allowedChars = buildAllowedCharSet(userId, gameMode);
        if (!allowedChars.isEmpty()) {
            words = words.stream()
                    .filter(w -> {
                        for (int i = 0; i < w.length(); i++) {
                            if (!allowedChars.contains(w.charAt(i))) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        // 已按「练习少/错误多」排好序，直接取前 limit 个，不再 shuffle
        return words.size() > limit ? words.subList(0, limit) : words;
    }

    private List<Character> buildCharPool(Integer userId, GameMode modeEnum) {
        List<Character> base = new ArrayList<>(buildBasePool(userId, modeEnum));
        List<Character> pool = new ArrayList<>(base);
        var weak = characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, WEAK_CHARS_LIMIT));
        for (var cs : weak) {
            if (cs.getCharValue() != null && !cs.getCharValue().isEmpty()) {
                for (int i = 0; i < 3; i++) pool.add(cs.getCharValue().charAt(0));
            }
        }
        return pool.isEmpty() ? new ArrayList<>(DEFAULT_CHAR_POOL) : pool;
    }

    private List<String> getLatestParentStrings(Integer userId) {
        var list = stringParentRepository.findByUserIdOrderByUpdateTimeDesc(userId, PageRequest.of(0, 1));
        if (list.isEmpty()) return java.util.Collections.emptyList();
        String joined = list.get(0).getStrParents();
        if (joined == null || joined.isEmpty()) return java.util.Collections.emptyList();
        return Arrays.stream(joined.split("\\|"))
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<Character> buildBasePool(Integer userId, GameMode modeEnum) {
        String charType = DEFAULT_CHAR_TYPES;
        try {
            var sysMode = gameModeConfigRepository.findByModeName(modeEnum.name());
            if (sysMode.isPresent()) {
                Integer modeId = sysMode.get().getId();
                GameCharTypeConfig cfg = gameCharTypeConfigRepository
                        .findByUserIdAndGameModeId(userId, modeId)
                        .orElse(null);
                if (cfg != null && cfg.getCharType() != null && !cfg.getCharType().trim().isEmpty()) {
                    charType = cfg.getCharType();
                }
            }
        } catch (Exception ignored) {
        }
        Set<String> types = parseTypes(charType);
        if (types.isEmpty()) types = parseTypes(DEFAULT_CHAR_TYPES);

        List<Character> base = new ArrayList<>();
        if (types.contains("lower")) for (char c = 'a'; c <= 'z'; c++) base.add(c);
        if (types.contains("upper")) for (char c = 'A'; c <= 'Z'; c++) base.add(c);
        if (types.contains("number")) for (char c = '0'; c <= '9'; c++) base.add(c);
        if (types.contains("punct")) {
            for (char c : new char[]{',', '.', '!', '?', ';', ':', '\'', '"'}) base.add(c);
        }
        if (types.contains("operator")) {
            for (char c : new char[]{'+', '-', '*', '/', '=', '<', '>', '(', ')', '[', ']', '{', '}'}) base.add(c);
        }
        if (types.contains("special")) {
            for (char c : new char[]{'@', '#', '$', '%', '^', '&', '_', '~', '\\', '|'}) base.add(c);
        }
        if (types.contains("chinese")) {
            String common = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞";
            for (int i = 0; i < common.length(); i++) base.add(common.charAt(i));
        }
        if (base.isEmpty()) base.addAll(DEFAULT_CHAR_POOL);
        return base;
    }

    private Set<String> parseTypes(String charType) {
        if (charType == null) return Set.of();
        String raw = charType.trim();
        if (raw.isEmpty()) return Set.of();
        String[] parts = raw.split("[,，\\s]+");
        Set<String> allowed = Set.of("lower", "upper", "number", "punct", "operator", "special", "chinese");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim().toLowerCase();
            if (s.equals("numbers")) s = "number";
            if (s.equals("punctuation")) s = "punct";
            if (s.equals("operators")) s = "operator";
            if (s.equals("specialchars") || s.equals("special_char") || s.equals("special-char")) s = "special";
            if (allowed.contains(s)) out.add(s);
        }
        return out;
    }

    private Set<Character> buildAllowedCharSet(Integer userId, String gameMode) {
        GameMode modeEnum;
        try {
            modeEnum = gameMode != null && !gameMode.isEmpty()
                    ? GameMode.valueOf(gameMode)
                    : GameMode.ENDLESS_ADAPT;
        } catch (IllegalArgumentException e) {
            modeEnum = GameMode.ENDLESS_ADAPT;
        }
        String charType = DEFAULT_CHAR_TYPES;
        try {
            var sysMode = gameModeConfigRepository.findByModeName(modeEnum.name());
            if (sysMode.isPresent()) {
                Integer modeId = sysMode.get().getId();
                GameCharTypeConfig cfg = gameCharTypeConfigRepository
                        .findByUserIdAndGameModeId(userId, modeId)
                        .orElse(null);
                if (cfg != null && cfg.getCharType() != null && !cfg.getCharType().trim().isEmpty()) {
                    charType = cfg.getCharType();
                }
            }
        } catch (Exception ignored) {
        }
        Set<String> types = parseTypes(charType);
        if (types.isEmpty()) types = parseTypes(DEFAULT_CHAR_TYPES);
        Set<Character> allowed = new HashSet<>();
        if (types.contains("lower")) for (char c = 'a'; c <= 'z'; c++) allowed.add(c);
        if (types.contains("upper")) for (char c = 'A'; c <= 'Z'; c++) allowed.add(c);
        if (types.contains("number")) for (char c = '0'; c <= '9'; c++) allowed.add(c);
        if (types.contains("punct")) {
            for (char c : new char[]{',', '.', '!', '?', ';', ':', '\'', '"'}) allowed.add(c);
        }
        if (types.contains("operator")) {
            for (char c : new char[]{'+', '-', '*', '/', '=', '<', '>', '(', ')', '[', ']', '{', '}'}) allowed.add(c);
        }
        if (types.contains("special")) {
            for (char c : new char[]{'@', '#', '$', '%', '^', '&', '_', '~', '\\', '|'}) allowed.add(c);
        }
        if (types.contains("chinese")) {
            String common = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞";
            for (int i = 0; i < common.length(); i++) allowed.add(common.charAt(i));
        }
        return allowed;
    }
}
