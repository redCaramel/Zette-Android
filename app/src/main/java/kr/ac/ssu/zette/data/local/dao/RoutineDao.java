package kr.ac.ssu.zette.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

import kr.ac.ssu.zette.data.local.entity.RoutineEntity;

@Dao
public abstract class RoutineDao {

    @Insert
    public abstract long insert(RoutineEntity entity);

    @Update
    public abstract int update(RoutineEntity entity);

    @Nullable
    @Query("SELECT * FROM routine WHERE id = :id")
    public abstract RoutineEntity findById(long id);

    // 회차 계산용: 소프트 삭제된 루틴 포함
    @Query("SELECT * FROM routine")
    public abstract List<RoutineEntity> getAll();

    @Query("SELECT * FROM routine")
    public abstract LiveData<List<RoutineEntity>> observeAll();

    // 루틴 목록 화면용: 삭제되지 않은 루틴
    @Query("SELECT * FROM routine WHERE deleted_at IS NULL ORDER BY arrival_time, name, id")
    public abstract LiveData<List<RoutineEntity>> observeNotDeleted();

    // 백그라운드용: 켜져 있고 삭제되지 않은 루틴
    @Query("SELECT * FROM routine WHERE activated = 1 AND deleted_at IS NULL ORDER BY arrival_time, name, id")
    public abstract List<RoutineEntity> getActive();

    @Query("UPDATE routine SET activated = :on WHERE id = :id")
    public abstract int setActivated(long id, boolean on);

    @Query("UPDATE routine SET deleted_at = :deletedAt WHERE id = :id")
    public abstract int setDeletedAt(long id, LocalDate deletedAt);
}
