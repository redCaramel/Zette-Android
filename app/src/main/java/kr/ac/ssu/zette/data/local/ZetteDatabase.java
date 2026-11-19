package kr.ac.ssu.zette.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.ssu.zette.data.local.dao.RoutineDao;
import kr.ac.ssu.zette.data.local.dao.ScheduleDao;
import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;

@Database(
        entities = {ScheduleEntity.class, RoutineEntity.class},
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class ZetteDatabase extends RoomDatabase {

    private static final String DB_NAME = "zette.db";

    // DB 쓰기·동기 조회용 작업 스레드
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public abstract ScheduleDao scheduleDao();

    public abstract RoutineDao routineDao();

    // TODO - 시연용 빌드 전 fallbackToDestructiveMigration 제거하고 Migration 작성
    @NonNull
    static ZetteDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), ZetteDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration(true)
                .build();
    }

    @NonNull
    public static ZetteDatabase createInMemory(@NonNull Context context) {
        return Room.inMemoryDatabaseBuilder(context.getApplicationContext(), ZetteDatabase.class)
                .allowMainThreadQueries()
                .build();
    }
}
