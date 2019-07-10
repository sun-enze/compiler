grammar emxstar;

program
	:	(func | clas | vari)*;

func
    :   type Id '(' (type Id (',' type Id)*)? ')' block;

funccon
    :   Id '('')' block;
    
vari
	:	type Id ('=' expression)? ';' ;
	
clas
	:	Class Id '{' (func|vari)* funccon? (func|vari)* '}';

type
	:	((Int | Bool | Id | String) ('['']')*) | Void;
	
block
	:	'{' (statement )* '}';
	
statement
    :   block																	#s1
    |  	expression ';'															#s2
    |	If '(' expression ')' statement (Else statement)?						#s3
    |	While '(' expression ')' statement										#s4
    |   For '(' expression? ';' expression? ';' expression? ')' statement		#s5
    |	Continue ';' 															#s6
    | 	Break ';'																#s6
    |	Return expression? ';'													#s6
    |	vari																	#s7
    |	';' 																	#s2
    ;
    
expression
    :   expression op=('++' | '--')												#e1
    |   expression '.' '('* Id ')'*												#e2
    |   expression '[' expression ']'											#e3
    |   expression '(' (expression (',' expression)*)? ')'						#e4
    |   <assoc=right> op=('++' | '--' | '+' | '-' | '!' | '~') expression		#e5
    |   New (Id | Int | String | Bool) ('[' expression? ']')* ('('')')?			#e6
    |   lhs=expression op=('*' | '/' | '%') rhs=expression						#e7
    |   lhs=expression op=('+' | '-') rhs=expression							#e7
    |   lhs=expression op=('<<'|'>>') rhs=expression							#e7
    |   lhs=expression op=('<' | '>' | '<=' | '>=') rhs=expression				#e7
    |   lhs=expression op=('=='|'!=') rhs=expression							#e7
    |   lhs=expression op='&' rhs=expression									#e7
    |   lhs=expression op='^' rhs=expression									#e7
    |   lhs=expression op='|' rhs=expression									#e7
    |   lhs=expression op='&&' rhs=expression									#e7
    |   lhs=expression op='||' rhs=expression									#e7
    |   <assoc=right> lhs=expression op='=' rhs=expression						#e7
    |   name																	#e8
    |   constant																#e8
    |	'(' expression ')'														#e9
    ;
constant
	:	True | False
	|	Stringcon
	|	Number
    |	Null
    |	This
	;
name
	: Id
	;
Bool                : 'bool';
Int                 : 'int';
String              : 'string';
Null       			: 'null';
Void                : 'void';
True       			: 'true';
False		      	: 'false';
If                  : 'if';
Else                : 'else';
For                 : 'for';
While               : 'while';
Break               : 'break';
Continue            : 'continue';
Return              : 'return';
New                 : 'new';
Class               : 'class';
This                : 'this';

Id
	:	[a-zA-Z][0-9a-zA-Z_]*
	;
	
Stringcon:
	'"'Char*'"'
;

fragment Char:
	~["\\\r\n]
	|'\\'["n\\r]
;
Number
	:	[1-9] [0-9]* | '0'
	;
    
WhiteSpace
    :   [ \t]+ -> skip
    ;

NewLine
    :   '\r'? '\n' -> skip
    ;

LineComment
    :   '//' ~[\r\n]* -> skip
    ;


