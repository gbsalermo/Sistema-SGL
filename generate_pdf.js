const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

// Read markdown file
const mdFile = path.join(__dirname, 'CONTINUIDADE.md');
const content = fs.readFileSync(mdFile, 'utf8');

// Create PDF
const pdfFile = path.join(__dirname, 'CONTINUIDADE.pdf');
const doc = new PDFDocument({
    size: 'A4',
    margin: 50,
    info: {
        Title: 'Projeto STOCK - Continuidade',
        Author: 'MimoCode',
        Subject: 'Documentação do Projeto'
    }
});

const stream = fs.createWriteStream(pdfFile);
doc.pipe(stream);

// Helper function to clean emojis
function cleanEmoji(text) {
    return text.replace(/[\u{1F600}-\u{1F64F}\u{1F300}-\u{1F5FF}\u{1F680}-\u{1F6FF}\u{1F1E0}-\u{1F1FF}\u{2702}-\u{27B0}\u{24C2}-\u{1F251}\u{1f926}-\u{1f937}\u{10000}-\u{10ffff}\u{2640}-\u{2642}\u{2600}-\u{2B55}\u{200d}\u{23cf}\u{23e9}\u{231a}\ufe0f\u{3030}]/gu, '').trim();
}

// Process content
const lines = content.split('\n');
let inCodeBlock = false;
let codeContent = [];

// Colors
const colors = {
    title: '#1a5276',
    h1: '#2e86c1',
    h2: '#5499c7',
    body: '#333333',
    code: '#666666'
};

// Font sizes
const fontSizes = {
    title: 24,
    h1: 18,
    h2: 14,
    h3: 12,
    body: 10,
    code: 9
};

let yPos = 50;

for (const line of lines) {
    // Handle code blocks
    if (line.trim().startsWith('```')) {
        if (inCodeBlock) {
            // End of code block - render code
            if (codeContent.length > 0) {
                yPos += 10;
                doc.font('Courier')
                    .fontSize(fontSizes.code)
                    .fillColor(colors.code);
                
                for (const codeLine of codeContent) {
                    if (yPos > 750) {
                        doc.addPage();
                        yPos = 50;
                    }
                    doc.text(codeLine, 60, yPos);
                    yPos += 12;
                }
                yPos += 10;
            }
            codeContent = [];
            inCodeBlock = false;
        } else {
            inCodeBlock = true;
        }
        continue;
    }
    
    if (inCodeBlock) {
        codeContent.push(line);
        continue;
    }
    
    // Skip empty lines
    if (!line.trim()) {
        yPos += 8;
        continue;
    }
    
    // Clean emojis
    const cleanLine = cleanEmoji(line);
    if (!cleanLine) continue;
    
    // Check if we need a new page
    if (yPos > 750) {
        doc.addPage();
        yPos = 50;
    }
    
    // Headers
    if (cleanLine.startsWith('# ') && !cleanLine.startsWith('## ')) {
        yPos += 20;
        doc.font('Helvetica-Bold')
            .fontSize(fontSizes.title)
            .fillColor(colors.title)
            .text(cleanLine.substring(2), 50, yPos);
        yPos += 30;
    } else if (cleanLine.startsWith('## ')) {
        yPos += 15;
        doc.font('Helvetica-Bold')
            .fontSize(fontSizes.h1)
            .fillColor(colors.h1)
            .text(cleanLine.substring(3), 50, yPos);
        yPos += 25;
    } else if (cleanLine.startsWith('### ')) {
        yPos += 10;
        doc.font('Helvetica-Bold')
            .fontSize(fontSizes.h2)
            .fillColor(colors.h2)
            .text(cleanLine.substring(4), 50, yPos);
        yPos += 20;
    } else if (cleanLine.startsWith('#### ')) {
        yPos += 8;
        doc.font('Helvetica-Bold')
            .fontSize(fontSizes.h3)
            .fillColor(colors.body)
            .text(cleanLine.substring(5), 50, yPos);
        yPos += 18;
    }
    // Horizontal rules
    else if (['---', '***', '___'].includes(cleanLine.trim())) {
        yPos += 10;
        doc.moveTo(50, yPos)
            .lineTo(545, yPos)
            .stroke('#cccccc');
        yPos += 10;
    }
    // Lists
    else if (cleanLine.startsWith('- ') || cleanLine.startsWith('* ')) {
        doc.font('Helvetica')
            .fontSize(fontSizes.body)
            .fillColor(colors.body)
            .text('  • ' + cleanLine.substring(2), 60, yPos);
        yPos += 14;
    }
    // Numbered lists
    else if (/^\d+\./.test(cleanLine)) {
        doc.font('Helvetica')
            .fontSize(fontSizes.body)
            .fillColor(colors.body)
            .text('  ' + cleanLine, 60, yPos);
        yPos += 14;
    }
    // Code inline
    else if (cleanLine.includes('`')) {
        const parts = cleanLine.split('`');
        let xPos = 60;
        
        for (let i = 0; i < parts.length; i++) {
            if (i % 2 === 0) {
                // Normal text
                if (parts[i]) {
                    doc.font('Helvetica')
                        .fontSize(fontSizes.body)
                        .fillColor(colors.body)
                        .text(parts[i], xPos, yPos, { continued: true });
                }
            } else {
                // Code text
                if (parts[i]) {
                    doc.font('Courier')
                        .fontSize(fontSizes.code)
                        .fillColor(colors.code)
                        .text(parts[i], xPos, yPos, { continued: true });
                }
            }
        }
        yPos += 14;
    }
    // Regular text
    else {
        // Handle bold
        let text = cleanLine;
        let isBold = false;
        
        if (text.includes('**')) {
            text = text.replace(/\*\*/g, '');
            isBold = true;
        }
        
        doc.font(isBold ? 'Helvetica-Bold' : 'Helvetica')
            .fontSize(fontSizes.body)
            .fillColor(colors.body)
            .text(text, 60, yPos, { width: 480 });
        
        // Calculate height based on text length
        const textHeight = Math.ceil(text.length / 80) * 14;
        yPos += textHeight + 4;
    }
}

// Finalize PDF
doc.end();

stream.on('finish', () => {
    console.log(`PDF gerado com sucesso: ${pdfFile}`);
});

stream.on('error', (err) => {
    console.error('Erro ao gerar PDF:', err);
});
