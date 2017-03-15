import sys
import math


def insertInto(inter,e,i,l):
    if inter!=e:
        if (l) == 1:
            return e

        p = i//(l//s);
        m = i%(l//s);

        
        try:
            inter[p] = insertInto(inter[p],e,m,l//s)
            ret = inter
        except:
            n = [inter]*s
            n[p] = insertInto(n[p],e,m,l//s)
            ret = n

        equal = True
        for p in ret:
            if p!=ret[0]:
                equal = False

        if equal:
            ret = ret[0]

        return ret
            
    else:
        return inter


a = []
for line in sys.stdin:
    i = int(line)
    a.append(i)
    

s = 8
l = s**int(math.ceil(math.log(len(a))/math.log(s) ))
interval = 1

for i,aa in enumerate(a):
    interval = insertInto(interval,aa,i,l)

print(interval)
