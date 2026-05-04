import os, re, glob

files = glob.glob('src/**/*.java', recursive=True)
count = 0

SHOW_ALERT_PATTERN = re.compile(
    r'private void showAlert\(String title, String message, Alert\.AlertType type\) \{'
    r'[^}]*Alert alert = new Alert\(type\);[^}]*alert\.setTitle\(title\);'
    r'[^}]*alert\.setHeaderText\(null\);[^}]*alert\.setContentText\(message\);'
    r'[^}]*alert\.showAndWait\(\);[^}]*\}',
    re.DOTALL
)

SHOW_ALERT_REPLACEMENT = (
    'private void showAlert(String title, String message, Alert.AlertType type) {\n'
    '        ModernAlert.Type mType = (type == Alert.AlertType.ERROR) ? ModernAlert.Type.ERROR :\n'
    '                                 (type == Alert.AlertType.WARNING) ? ModernAlert.Type.WARNING :\n'
    '                                 ModernAlert.Type.INFO;\n'
    '        ModernAlert.show(mType, title, message);\n'
    '    }'
)

for f in files:
    with open(f, encoding='utf-8', errors='ignore') as fh:
        content = fh.read()

    if 'new Alert(' not in content and 'Alert.AlertType' not in content:
        continue

    orig = content

    # Add import if needed
    if 'import org.example.util.ModernAlert;' not in content:
        if 'import javafx.scene.control.*;' in content:
            content = content.replace(
                'import javafx.scene.control.*;',
                'import javafx.scene.control.*;\nimport org.example.util.ModernAlert;'
            )
        else:
            content = re.sub(
                r'(package [^;]+;)',
                r'\1\nimport org.example.util.ModernAlert;',
                content, count=1
            )

    # Replace showAlert helper body
    content = SHOW_ALERT_PATTERN.sub(SHOW_ALERT_REPLACEMENT, content)

    # One-liner INFO
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.INFORMATION,\s*("(?:[^"\\]|\\.)*")\s*\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.INFO, "Info", \1);',
        content
    )
    # One-liner ERROR
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.ERROR,\s*("(?:[^"\\]|\\.)*")\s*\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.ERROR, "Error", \1);',
        content
    )
    # One-liner WARNING
    content = re.sub(
        r'new Alert\(Alert\.AlertType\.WARNING,\s*("(?:[^"\\]|\\.)*")\s*\)\.showAndWait\(\);',
        r'ModernAlert.show(ModernAlert.Type.WARNING, "Warning", \1);',
        content
    )

    if content != orig:
        with open(f, 'w', encoding='utf-8') as fh:
            fh.write(content)
        count += 1
        print('Updated:', f)

print('Total files updated:', count)
