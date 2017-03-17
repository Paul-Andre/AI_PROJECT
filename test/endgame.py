yes = {}

def change(inp):
    (pointDiff,conf) = inp
    return (-pointDiff, conf[6:12]+conf[0:6])




#partition n into sums of m non-negative integers
def partition(n,m):
    if m == 1 :
        yield (n,)
    else:
        for i in range(0,n+1):
            for j in partition(n-i,m-1):
                yield (i,)+j


def sow(inp,n):
    l = list(inp[1])
    beans = l[n]
    l[n] = 0
    i = n
    while beans!=0 :
        i = (i+1)%12
        l[i] += 1
        beans-=1

    seized = 0
    while l[i] in [2,4,6]:
        seized += l[i]
        l[i] = 0
        i = (i-1)%12

    return (inp[0]+seized, tuple(l))


pr = False


def getSimple(inp):


    #print("Getting simple",inp)
    summ = sum(inp[1])
    #print(summ)
    diff = inp[0]

    if (diff-summ>0) :
        return 1

    if (diff+summ<0) :
        return  -1

    if (summ == 0 and diff==0) :
        return  0

    if sum(inp[1][6:12]) == 0 :
        if inp[0]+sum(inp[1]) > 0:
            return 1
        elif inp[0]+sum(inp[1]) < 0:
            return -1
        else:
            return 0

    if inp in yes and yes[inp] != "involved":
        return yes[inp]


    if inp[0]-2>=0 and getSimple((inp[0]-2,inp[1])) == 1:
        return 1

    #if inp[0]<0:
        #return getSimple(change(inp))

    return None

def isInvolved(inp):
    return inp in yes and yes[inp] == "involved"


def getAndCache(inp_):

    simple = getSimple(inp_)
    if (simple!=None) :
        return (simple,0)

    stack = []
    stack.append((inp_,False))

    itera = 0
    while(len(stack)!=0) :
        #print(len(stack))
        inp, doIt = stack.pop();
        #print("iteration",itera,"with input",inp)
        #print(inp,"to do it:",doIt);
        itera+=1

        simple = getSimple(inp)
        if (simple!=None) :
            continue

        else:
            yes[inp] = "involved"
            ##print("become involved")

            if not doIt:
                #print("not doing it")

                inpp = change(inp)
                known = 0
                m = -10000
                for i in range(0,6):
                    if inpp[1][i]!=0 :
                        sowed = sow(inpp,i)

                        got = getSimple(sowed)
                        #print("sowed", i, sowed, "got",got)
                        if got != None:
                            known+=1
                            if got == 1:
                                m = 1
                                break
                            else:
                                m = max(m, got)
                    else:
                        known+=1
                if m == 1 or (known == 6 and m!=-10000) :
                    yes[inp] = -m
                    #print("yes")
                else:
                    #print("Need to redo stuff")
                    stack.append((inp,True))
                    for i in range(0,6):
                        if inpp[1][i]!=0 :
                            sowed = sow(inpp,i)
                            got = getSimple(sowed)
                            if got == None and not isInvolved(sowed):
                                stack.append((sowed,False))
                                ##print("appending", sowed)

            else:
                #print("doing it")
                inpp = change(inp)
                m = -10000
                for i in range(0,6):
                    if inpp[1][i]!=0 :
                        sowed = (sow(inpp,i))
                        got = getSimple(sowed);
                        if got != None:
                            m = max (got, m)
                            if m == 1:
                                break

                if m == -10000 :
                    #print("Not done, so delete")
                    del yes[inp]
                    continue
                ret = -m
                yes[inp] = ret
                #print("something else")


    #print(yes)
    return yes[inp],itera


#print(len(list(partition(6,12))))
bigarray = []
if True:
    for i in range(0,11):
        start = i%2
        for j in range(start,(i)+1,2):
            for part in partition(i,12):
                inp = (j,part)
                #print(inp)
                got,it  = getAndCache(inp)
                #bigarray.append(got)
                print(i,it,"wew",got)
                #print("If after a move we get",inp,"we will win",got)
                if (got == "involved") :
                    print(yes)
                    exit(0)

#before = (3, (2, 2, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0))
#print(before)
#print(getAndCache(before,1))

#print(len(bigarray))




#for partt in partition(7,10):
#    for i in range(-1,0,2):
#        part = (0,0) + partt
#        inp = (i,part)
#        if (part[0] == 1):
#            exit()
#        #print(inp)
#        got  = getAndCache(inp,1)
#        print(-i,"If after a move we get",inp,"we will win",got)

#print(yes)
