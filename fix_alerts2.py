import os, re, glob

files = glob.glob('src/**/*.java', recursive=True)
total = 0

def ensure_import(content):
    if 'import org.example.util.ModernAlert;' in content:
        return content
    if 'import javafx.scene.control.*;' in content:
        return content.replace(
            'import javafx.scene.control.*;',
            'import javafx.scene.control.*;\nimport org.example.util.ModernAlert;'
        )
    return re.sub(r'(package [^;]+;)', r'\1\nimport org.example.util.ModernAlert;', content, count=1)

for f in files:
    with open(f, encoding='utf-8', errors='ignore') as fh:
        content = fh.read()

    if 'new Alert(' not in content:
        continue

    orig = content
    content = ensure_import(content)

    # ── showAlert(String msg) helpers ─────────────────────────────────────────
    content = re.sub(
        r'private void showAlert\(String msg\)\s*\{[^}]*new Alert\(Alert\.AlertType\.WARNING,\s*msg[^}]*\}',
        'private void showAlert(String msg) { ModernAlert.show(ModernAlert.Type.WARNING, "Warning", msg); }',
        content, flags=re.DOTALL
    )
    content = re.sub(
        r'private void showInfo\(String msg\)\s*\{[^}]*new Alert\(Alert\.AlertType\.INFORMATION,\s*msg[^}]*\}',
        'private void showInfo(String msg) { ModernAlert.show(ModernAlert.Type.INFO, "Info", msg); }',
        content, flags=re.DOTALL
    )

    # ── showAlert(String msg) one-liner ───────────────────────────────────────
    content = re.sub(
        r'private void showAlert\(String msg\)\{new Alert\(Alert\.AlertType\.WARNING,msg,ButtonType\.OK\)\.showAndWait\(\);\}',
        'private void showAlert(String msg) { ModernAlert.show(ModernAlert.Type.WARNING, "Warning", msg); }',
        content
    )

    # ── Multi-line Alert blocks (WARNING/INFO/ERROR) ──────────────────────────
    def replace_alert_block(m):
        atype = m.group(1)
        title = m.group(2) or 'Info'
        msg   = m.group(3) or '""'
        mtype = {'WARNING': 'WARNING', 'INFORMATION': 'INFO', 'ERROR': 'ERROR'}.get(atype, 'INFO')
        return f'ModernAlert.show(ModernAlert.Type.{mtype}, {title}, {msg});'

    # Pattern: Alert x = new Alert(AlertType.XXX); x.setTitle("T"); x.setHeaderText(...); x.setContentText(MSG); x.showAndWait();
    content = re.sub(
        r'Alert \w+ = new Alert\(Alert\.AlertType\.(WARNING|INFORMATION|ERROR)\);\s*'
        r'\w+\.setTitle\(("(?:[^"\\]|\\.)*")\);\s*'
        r'\w+\.setHeaderText\([^)]*\);\s*'
        r'\w+\.setContentText\(([^;]+)\);\s*'
        r'\w+\.showAndWait\(\);',
        replace_alert_block,
        content, flags=re.DOTALL
    )

    # Pattern without setTitle
    content = re.sub(
        r'Alert \w+ = new Alert\(Alert\.AlertType\.(WARNING|INFORMATION|ERROR)\);\s*'
        r'\w+\.setContentText\(([^;]+)\);\s*'
        r'\w+\.showAndWait\(\);',
        lambda m: f'ModernAlert.show(ModernAlert.Type.{"WARNING" if m.group(1)=="WARNING" else "INFO" if m.group(1)=="INFORMATION" else "ERROR"}, "{"Warning" if m.group(1)=="WARNING" else "Info" if m.group(1)=="INFORMATION" else "Error"}", {m.group(2)});',
        content, flags=re.DOTALL
    )

    # ── CONFIRMATION alerts → ModernAlert.confirm ─────────────────────────────
    # Pattern: Alert c = new Alert(CONFIRMATION, "msg", YES, NO); c.setHeaderText(null); c.showAndWait().ifPresent(btn -> { if(btn==YES) { ... } });
    # We replace the Alert creation + showAndWait with ModernAlert.confirm
    content = re.sub(
        r'Alert \w+=new Alert\(Alert\.AlertType\.CONFIRMATION,"([^"]+)",ButtonType\.YES,ButtonType\.NO\);\s*'
        r'\w+\.setHeaderText\(null\);\s*'
        r'\w+\.showAndWait\(\)\.ifPresent\(btn->\{ if\(btn==ButtonType\.YES\) \{',
        r'if (ModernAlert.confirm("Confirmation", "\1")) {',
        content
    )

    # Pattern: Alert confirm = new Alert(CONFIRMATION, "msg", YES, NO); confirm.showAndWait().ifPresent(btn -> { if(btn==YES) {
    content = re.sub(
        r'Alert \w+ = new Alert\(Alert\.AlertType\.CONFIRMATION,\s*"([^"]+)",\s*ButtonType\.YES,\s*ButtonType\.NO\);\s*'
        r'\w+\.showAndWait\(\)\.ifPresent\(btn -> \{\s*if \(btn == ButtonType\.YES\) \{',
        r'if (ModernAlert.confirm("Confirmation", "\1")) {',
        content
    )

    # Pattern: Alert confirm = new Alert(CONFIRMATION); confirm.setTitle("T"); confirm.setHeaderText("H"); Optional<ButtonType> result = confirm.showAndWait(); if (result.isPresent() && result.get() == ButtonType.OK) {
    content = re.sub(
        r'Alert \w+ = new Alert\(Alert\.AlertType\.CONFIRMATION\);\s*'
        r'\w+\.setTitle\("([^"]+)"\);\s*'
        r'\w+\.setHeaderText\("([^"]+)"\);\s*'
        r'Optional<ButtonType> \w+ = \w+\.showAndWait\(\);\s*'
        r'if \(\w+\.isPresent\(\) && \w+\.get\(\) == ButtonType\.OK\) \{',
        r'if (ModernAlert.confirm("\1", "\2")) {',
        content, flags=re.DOTALL
    )

    # Pattern: Alert confirm = new Alert(CONFIRMATION); confirm.setTitle("T"); confirm.setHeaderText("H"); confirm.showAndWait().ifPresent(btn -> { if (btn == ButtonType.OK) {
    content = re.sub(
        r'Alert \w+ = new Alert\(Alert\.AlertType\.CONFIRMATION\);\s*'
        r'\w+\.setTitle\("([^"]+)"\);\s*'
        r'\w+\.setHeaderText\("([^"]+)"\);\s*'
        r'\w+\.showAndWait\(\)\.ifPresent\(\w+ -> \{\s*if \(\w+ == ButtonType\.OK\) \{',
        r'if (ModernAlert.confirm("\1", "\2")) {',
        content, flags=re.DOTALL
    )

    # ── Task delete one-liner ─────────────────────────────────────────────────
    content = re.sub(
        r'Alert confirm = new Alert\(Alert\.AlertType\.CONFIRMATION, "Delete task\?", ButtonType\.YES, ButtonType\.NO\);\s*'
        r'\w+\.showAndWait\(\)\.ifPresent\(\w+ -> \{\s*if \(\w+ == ButtonType\.YES\) \{',
        r'if (ModernAlert.confirm("Delete Task", "Delete this task?")) {',
        content, flags=re.DOTALL
    )

    # ── Sprint delete ─────────────────────────────────────────────────────────
    content = re.sub(
        r'Alert confirm = new Alert\(Alert\.AlertType\.CONFIRMATION,\s*\n\s*"Delete sprint[^"]*",\s*\n\s*ButtonType\.YES, ButtonType\.NO\);\s*'
        r'\w+\.showAndWait\(\)\.ifPresent\(\w+ -> \{\s*if \(\w+ == ButtonType\.YES\) \{',
        r'if (ModernAlert.confirm("Delete Sprint", "Delete this sprint? All tasks will also be deleted.")) {',
        content, flags=re.DOTALL
    )

    # ── MyCandidatures PDF alerts ─────────────────────────────────────────────
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.INFORMATION, "PDF exported:\\n" \+ file\.getAbsolutePath\(\)\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.SUCCESS, "PDF Exported", "PDF exported:\\n" + file.getAbsolutePath());',
        content
    )
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.ERROR, "PDF error: " \+ e\.getMessage\(\)\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.ERROR, "PDF Error", "PDF error: " + e.getMessage());',
        content
    )

    # ── SprintStatsController ─────────────────────────────────────────────────
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.ERROR, "Load error: " \+ e\.getMessage\(\)\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Load error: " + e.getMessage());',
        content
    )
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.ERROR, "Export error: " \+ e\.getMessage\(\)\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Export error: " + e.getMessage());',
        content
    )

    # ── MeetingController ─────────────────────────────────────────────────────
    content = re.sub(
        r'private void showAlert\(String msg\)\{new Alert\(Alert\.AlertType\.WARNING,msg,ButtonType\.OK\)\.showAndWait\(\);\}',
        r'private void showAlert(String msg) { ModernAlert.show(ModernAlert.Type.WARNING, "Warning", msg); }',
        content
    )

    if content != orig:
        with open(f, 'w', encoding='utf-8') as fh:
            fh.write(content)
        total += 1
        print('Updated:', f)

print('Total:', total)
