import re
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm, inch
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Preformatted
from reportlab.lib.enums import TA_LEFT, TA_CENTER

def clean_emoji(text):
    """Remove emojis and special unicode characters for PDF compatibility"""
    emoji_pattern = re.compile("["
        u"\U0001F600-\U0001F64F"
        u"\U0001F300-\U0001F5FF"
        u"\U0001F680-\U0001F6FF"
        u"\U0001F1E0-\U0001F1FF"
        u"\U00002702-\U000027B0"
        u"\U000024C2-\U0001F251"
        u"\U0001f926-\U0001f937"
        u"\U00010000-\U0010ffff"
        u"\u2640-\u2642"
        u"\u2600-\u2B55"
        u"\u200d"
        u"\u23cf"
        u"\u23e9"
        u"\u231a"
        u"\ufe0f"
        u"\u3030"
        "]+", flags=re.UNICODE)
    return emoji_pattern.sub(r'', text).strip()

def md_to_pdf(md_file, pdf_file):
    # Read markdown file
    with open(md_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Create PDF
    doc = SimpleDocTemplate(
        pdf_file,
        pagesize=A4,
        leftMargin=20*mm,
        rightMargin=20*mm,
        topMargin=20*mm,
        bottomMargin=20*mm
    )
    
    styles = getSampleStyleSheet()
    
    # Custom styles
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Title'],
        fontSize=24,
        spaceAfter=20,
        alignment=TA_CENTER
    )
    
    h1_style = ParagraphStyle(
        'H1',
        parent=styles['Heading1'],
        fontSize=18,
        spaceBefore=20,
        spaceAfter=10,
        textColor=colors.HexColor('#1a5276')
    )
    
    h2_style = ParagraphStyle(
        'H2',
        parent=styles['Heading2'],
        fontSize=14,
        spaceBefore=15,
        spaceAfter=8,
        textColor=colors.HexColor('#2e86c1')
    )
    
    h3_style = ParagraphStyle(
        'H3',
        parent=styles['Heading3'],
        fontSize=12,
        spaceBefore=10,
        spaceAfter=6,
        textColor=colors.HexColor('#5499c7')
    )
    
    body_style = ParagraphStyle(
        'CustomBody',
        parent=styles['BodyText'],
        fontSize=10,
        leading=14,
        spaceAfter=6
    )
    
    code_style = ParagraphStyle(
        'Code',
        parent=styles['Code'],
        fontSize=8,
        leading=10,
        fontName='Courier',
        backColor=colors.HexColor('#f4f4f4'),
        borderWidth=1,
        borderColor=colors.HexColor('#dddddd'),
        borderPadding=5
    )
    
    story = []
    
    # Process markdown line by line
    lines = content.split('\n')
    in_code_block = False
    code_content = []
    
    for line in lines:
        # Handle code blocks
        if line.strip().startswith('```'):
            if in_code_block:
                # End of code block
                code_text = '\n'.join(code_content)
                if code_text.strip():
                    story.append(Spacer(1, 5))
                    story.append(Preformatted(code_text, code_style))
                    story.append(Spacer(1, 5))
                code_content = []
                in_code_block = False
            else:
                # Start of code block
                in_code_block = True
            continue
        
        if in_code_block:
            code_content.append(line)
            continue
        
        # Skip empty lines
        if not line.strip():
            story.append(Spacer(1, 6))
            continue
        
        # Clean emojis
        clean_line = clean_emoji(line)
        if not clean_line:
            continue
        
        # Headers
        if clean_line.startswith('# ') and not clean_line.startswith('## '):
            text = clean_emoji(clean_line[2:])
            story.append(Paragraph(text, title_style))
        elif clean_line.startswith('## '):
            text = clean_emoji(clean_line[3:])
            story.append(Paragraph(text, h1_style))
        elif clean_line.startswith('### '):
            text = clean_emoji(clean_line[4:])
            story.append(Paragraph(text, h2_style))
        elif clean_line.startswith('#### '):
            text = clean_emoji(clean_line[5:])
            story.append(Paragraph(text, h3_style))
        # Horizontal rules
        elif clean_line.strip() in ['---', '***', '___']:
            story.append(Spacer(1, 10))
        # Lists
        elif clean_line.startswith('- ') or clean_line.startswith('* '):
            text = clean_emoji(clean_line[2:])
            story.append(Paragraph(f"  {text}", body_style))
        elif re.match(r'^\d+\.', clean_line):
            text = clean_emoji(clean_line)
            story.append(Paragraph(text, body_style))
        # Code inline (backticks)
        elif '`' in clean_line:
            # Replace backticks with font tags
            text = re.sub(r'`([^`]+)`', r'<font face="Courier" size="9">\1</font>', clean_line)
            text = clean_emoji(text)
            story.append(Paragraph(text, body_style))
        # Regular text
        else:
            # Handle bold and italic
            text = clean_line
            text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)
            text = re.sub(r'\*([^*]+)\*', r'<i>\1</i>', text)
            story.append(Paragraph(text, body_style))
    
    # Build PDF
    doc.build(story)
    print(f"PDF gerado com sucesso: {pdf_file}")

if __name__ == "__main__":
    md_file = r"C:\Users\07548262523\Documents\stock\CONTINUIDADE.md"
    pdf_file = r"C:\Users\07548262523\Documents\stock\CONTINUIDADE.pdf"
    md_to_pdf(md_file, pdf_file)
