package hu.mocsar.server.persistence;

import hu.mocsar.core.Role;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final RoundResultRepository roundResults;

    public StatsService(RoundResultRepository roundResults) {
        this.roundResults = roundResults;
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> leaderboard() {
        return aggregate(roundResults.countRolesByNickname());
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> leaderboard(Long gameId) {
        return aggregate(roundResults.countRolesByNicknameForGame(gameId));
    }

    static List<PlayerStats> aggregate(List<RoundResultRepository.RoleCount> counts) {
        Map<String, long[]> byNick = new LinkedHashMap<>();
        for (RoundResultRepository.RoleCount rc : counts) {
            long[] acc = byNick.computeIfAbsent(rc.nickname(), k -> new long[3]);
            acc[0] += rc.count();
            if (rc.role() == Role.KING) {
                acc[1] += rc.count();
            }
            if (rc.role() == Role.SWAMP) {
                acc[2] += rc.count();
            }
        }
        List<PlayerStats> stats = new ArrayList<>();
        byNick.forEach((nick, acc) -> stats.add(new PlayerStats(nick, acc[0], acc[1], acc[2])));
        stats.sort(Comparator.comparingLong(PlayerStats::kings).reversed()
                .thenComparingLong(PlayerStats::swamps)
                .thenComparing(PlayerStats::nickname));
        return stats;
    }
}
