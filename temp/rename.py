import os,re,shutil

t = '32'
p = re.compile('(speaker_\d)_(\d+)\.png')
for f in os.listdir('.'):
    m = p.search(f)
    if m:
        if m.group(2)==t:
            shutil.copy(f,m.group(1)+'.png')
        
