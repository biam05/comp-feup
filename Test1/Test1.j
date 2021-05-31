.class public Test1
.super java/lang/Object

.field 'a' I
.field 'b' [I
.field 'c' I

.method public <init>()V
	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public sum([II)I
		.limit locals 8
		.limit stack 2

		aload_0
		getfield Test1/a I
		istore_3

		iconst_0
		istore 4

		aload_1
		iload 4
		iaload
		istore 5

		iload_3
		iload 5
		iadd
		istore 6

		iload 6
		iload_2
		iadd
		istore 7

		iload 7
		
		ireturn
.end method

.method public sum(I)I
		.limit locals 4
		.limit stack 2

		aload_0
		getfield Test1/a I
		istore_2

		iload_2
		iload_1
		iadd
		istore_3

		iload_3
		
		ireturn
.end method

.method public sum(II)Z
		.limit locals 7
		.limit stack 2

		aload_0
		getfield Test1/a I
		istore_3

		iload_3
		iload_1

		if_icmpge ElseLTH0
		iconst_1
		istore 4

		goto AfterLTH0

	ElseLTH0:
		iconst_0
		istore 4


	AfterLTH0:
		iload_1
		iload_2

		if_icmpge ElseLTH1
		iconst_1
		istore 5

		goto AfterLTH1

	ElseLTH1:
		iconst_0
		istore 5


	AfterLTH1:
		iload 4
		iload 5
		iand
		istore 6

		iload 6
		
		ireturn
.end method

.method public getArray()[I
		.limit locals 2
		.limit stack 1

		aload_0
		getfield Test1/b [I
		astore_1

		aload_1
		
		areturn
.end method

.method public static main([Ljava/lang/String;)V
		.limit locals 9
		.limit stack 5

		iconst_3
		istore_2

		iconst_5
		istore_3

		new Test1
		dup
		invokespecial Test1.<init>()V
		astore 4

		aload 4
		invokevirtual Test1.buildArray()I
		pop
		aload 4
		iconst_3
		iconst_5
		invokevirtual Test1.sum(II)Z
		istore 5

		iload 5
		iconst_0
		if_icmpeq else1
		aload 4
		invokevirtual Test1.getArray()[I
		astore 6

		aload 4
		aload 6
		iload_3
		invokevirtual Test1.sum([II)I
		istore 7

		goto endif1

	else1:
		aload 4
		invokevirtual Test1.getArray()[I
		astore 8

		aload 8
		arraylength
		istore 7

	endif1:
		iload 7
		invokestatic io.println(I)V
		return
.end method

.method public buildArray()I
		.limit locals 4
		.limit stack 4

		aload_0
		iconst_2
		putfield Test1/a I

		iconst_2
		newarray int
		astore_1

		aload_0
		aload_1
		putfield Test1/b [I

		iconst_0
		istore_2

		aload_0
		getfield Test1/b [I
		astore_3

		aload_3
		iload_2
		iconst_1
		iastore

		aload_0
		iconst_5
		putfield Test1/c I

		iconst_5
		ireturn
.end method