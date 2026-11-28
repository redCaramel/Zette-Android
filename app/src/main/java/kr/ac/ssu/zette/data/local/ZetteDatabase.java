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
        exportSchema = true     // app/schemas 에 버전별 구조 저장 (Migration 작성용)
)
@TypeConverters(Converters.class)
public abstract class ZetteDatabase extends RoomDatabase {

    private static final String DB_NAME = "zette.db";

    // DB 쓰기·동기 조회용 작업 스레드
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public abstract ScheduleDao scheduleDao();

    public abstract RoutineDao routineDao();

    // version 을 올리면 Migration 을 만들어 addMigrations 에 넣는다 (없으면 앱이 데이터를 지우지 않고 멈춘다)
    @NonNull
    static ZetteDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), ZetteDatabase.class, DB_NAME)
                .build();
    }

    @NonNull
    public static ZetteDatabase createInMemory(@NonNull Context context) {
        return Room.inMemoryDatabaseBuilder(context.getApplicationContext(), ZetteDatabase.class)
                .allowMainThreadQueries()
                .build();
    }
}
