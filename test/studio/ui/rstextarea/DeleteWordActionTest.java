package studio.ui.rstextarea;

import org.junit.jupiter.api.Test;

import javax.swing.text.Segment;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteWordActionTest {

    private void assertBack(String text) {
        String[] parts = text.split("\\|", -1);

        assertEquals(3, parts.length);
        String actualText = parts[0] + parts[1] + parts[2];
        Segment segment = new Segment(
                actualText.toCharArray(),
                0,
                parts[0].length() + parts[1].length());

        int pos = DeleteWordAction.getStartOffsetForRemoval(segment);
        assertEquals(parts[0].length(), pos);
    }

    private void assertForward(String text) {
        String[] parts = text.split("\\|", -1);

        assertEquals(3, parts.length);
        String actualText = parts[0] + parts[1] + parts[2];
        Segment segment = new Segment(
                actualText.toCharArray(),
                parts[0].length(),
                parts[1].length() + parts[2].length());

        int pos = DeleteWordAction.getEndOffsetForRemoval(segment);
        assertEquals(parts[0].length() + parts[1].length(), pos);
    }


    @Test
    public void testBack() {
        assertBack("abc |def|gh");
        assertBack("abc   |def|gh");
        assertBack("   |def|gh");
        assertBack(" |def|gh");
        assertBack("|def|gh");
        assertBack("|d|gh");
        assertBack("||gh");
        assertBack("|def |gh");
        assertBack("|def  |gh");
        assertBack("|def  | gh");
        assertBack(" |def  | gh");
        assertBack(" |d  | gh");

        assertBack(" bd|,,| gh");
        assertBack(" bd|,.,| gh");
        assertBack(" bd|,.,| gh");
        assertBack(" bd |,.,| gh");

    }

    @Test
    public void testForward() {
        assertForward("abc d|efg |kl");
        assertForward("abc d|efg   |kl");
        assertForward("abc d|efgkl|");
        assertForward("abc d|efgkl |");
        assertForward("abc d| |efgkl");
        assertForward("abc d | |efgkl");
        assertForward("abc d |   |efgkl");
        assertForward("abc d |   |e ");
        assertForward("abc d    |e |b");

        assertForward("abc d |   |,e ");
        assertForward("abc d|,,...|efgkl ");
        assertForward("abc d|,,...|e,fgkl ");
        assertForward("abc d|,,...   |");
        assertForward("abc d|,,...   |a");
        assertForward("abc d|,,...   |,a");

    }

}
