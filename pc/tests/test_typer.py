import pytest
from src.typer import Typer


class TestTyperDiffLogic:
    """Test the diff/state logic without actually calling SendInput."""

    def test_common_prefix_length(self):
        t = Typer(dry_run=True)
        assert t._common_prefix_len("Hello", "Hello world") == 5
        assert t._common_prefix_len("Hello", "Help") == 3
        assert t._common_prefix_len("abc", "xyz") == 0
        assert t._common_prefix_len("", "hello") == 0
        assert t._common_prefix_len("same", "same") == 4

    def test_partial_first_word(self):
        t = Typer(dry_run=True)
        ops = t.handle_partial("Hey")
        assert ops == [("type", "Hey")]
        assert t._current_text == "Hey"

    def test_partial_grows(self):
        t = Typer(dry_run=True)
        t.handle_partial("Hey")
        ops = t.handle_partial("Hey Claude")
        assert ops == [("type", " Claude")]
        assert t._current_text == "Hey Claude"

    def test_partial_correction(self):
        t = Typer(dry_run=True)
        t.handle_partial("I think the air")
        ops = t.handle_partial("I think the error")
        # Should backspace "air" (3 chars) and type "error"
        assert ops == [("backspace", 3), ("type", "error")]

    def test_final_replaces_partial(self):
        t = Typer(dry_run=True)
        t.handle_partial("Hey Claude can you")
        ops = t.handle_final("Hey Claude, can you fix the login bug.")
        # Backspaces all 18 chars of partial, types final
        assert ops[0] == ("backspace", 18)
        assert ops[1] == ("type", "Hey Claude, can you fix the login bug.")

    def test_space_between_utterances(self):
        t = Typer(dry_run=True)
        t.handle_partial("First")
        t.handle_final("First.")
        ops = t.handle_partial("Second")
        # Should prepend space
        assert ops == [("type", " Second")]

    def test_undo(self):
        t = Typer(dry_run=True)
        t.handle_partial("Hello")
        t.handle_final("Hello.")
        ops = t.handle_undo()
        # Backspace "Hello." (6 chars)
        assert ops == [("backspace", 6)]

    def test_stop_resets(self):
        t = Typer(dry_run=True)
        t.handle_partial("Hello")
        t.handle_final("Hello.")
        t.handle_stop()
        # After stop, next session starts fresh (no space prepend)
        ops = t.handle_partial("New session")
        assert ops == [("type", "New session")]

    def test_terminal_mode_skips_partials(self):
        t = Typer(dry_run=True, terminal_mode=True)
        ops = t.handle_partial("Hey Claude")
        assert ops == []  # No typing on partials
        ops = t.handle_final("Hey Claude, can you fix the login bug.")
        assert ops == [("type", "Hey Claude, can you fix the login bug.")]
