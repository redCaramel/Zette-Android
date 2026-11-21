package kr.ac.ssu.zette.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OccurrenceChoiceDialogTest {

    @Test
    public void topicParticle_followsFinalConsonant() {
        assertEquals("는", OccurrenceChoiceDialog.topicParticle("출근 준비"));
        assertEquals("은", OccurrenceChoiceDialog.topicParticle("주말 운동"));
        assertEquals("는", OccurrenceChoiceDialog.topicParticle("Gym"));
        assertEquals("는", OccurrenceChoiceDialog.topicParticle(""));
    }
}
