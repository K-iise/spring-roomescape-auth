package roomescape.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.dao.StoreDao;
import roomescape.service.dto.result.StoreResult;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreDao storeDao;

    public StoreService(StoreDao storeDao) {
        this.storeDao = storeDao;
    }

    public Optional<StoreResult> findManagedStore(Long memberId) {
        return storeDao.findByManagerMemberId(memberId)
                .map(StoreResult::from);
    }
}
