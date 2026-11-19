package kr.ac.ssu.zette.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;

@Dao
public abstract class ScheduleDao {

    @Insert
    public abstract long insert(ScheduleEntity entity);

    @Update
    public abstract int update(ScheduleEntity entity);

    @Query("DELETE FROM schedule WHERE id = :id")
    public abstract int deleteById(long id);

    @Nullable
    @Query("SELECT * FROM schedule WHERE id = :id")
    public abstract ScheduleEntity findById(long id);

    @Nullable
    @Query("SELECT * FROM schedule WHERE routine_id = :routineId AND occurrence_date = :occurrenceDate")
    public abstract ScheduleEntity findOccurrence(long routineId, LocalDate occurrenceDate);

    @Query("SELECT * FROM schedule WHERE routine_id = :routineId")
    public abstract List<ScheduleEntity> getOccurrenceRows(long routineId);

    // 기간에 걸친 행: 표시 날짜(date)가 기간 안이거나, 회차의 원래 날짜가 기간 안인 행
    @Query("SELECT * FROM schedule"
            + " WHERE (date BETWEEN :from AND :to)"
            + " OR (occurrence_date BETWEEN :from AND :to)")
    public abstract List<ScheduleEntity> getInRange(LocalDate from, LocalDate to);

    @Query("SELECT * FROM schedule"
            + " WHERE (date BETWEEN :from AND :to)"
            + " OR (occurrence_date BETWEEN :from AND :to)")
    public abstract LiveData<List<ScheduleEntity>> observeInRange(LocalDate from, LocalDate to);

    @Query("DELETE FROM schedule WHERE routine_id = :routineId AND occurrence_date >= :from")
    public abstract int deleteOccurrencesFrom(long routineId, LocalDate from);

    @Query("SELECT COUNT(*) FROM schedule")
    public abstract int count();

    // schedule 테이블이 바뀔 때마다 값을 다시 내보내는 트리거용 LiveData
    @Query("SELECT COUNT(*) FROM schedule")
    public abstract LiveData<Integer> observeTableChanges();

    // 같은 (routine_id, occurrence_date) 회차 행은 하나만 유지한다. 반환값은 행 id.
    @Transaction
    public long upsertOccurrence(ScheduleEntity entity) {
        if (entity.routineId == null || entity.occurrenceDate == null) {
            throw new IllegalArgumentException("회차 행에는 routineId와 occurrenceDate가 필요합니다.");
        }
        ScheduleEntity existing = findOccurrence(entity.routineId, entity.occurrenceDate);
        if (existing == null) {
            entity.id = 0;
            return insert(entity);
        }
        entity.id = existing.id;
        update(entity);
        return existing.id;
    }
}
