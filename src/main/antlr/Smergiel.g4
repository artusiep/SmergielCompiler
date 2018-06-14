grammar Smergiel;

// Lexical Grammar

Begin   : 'begin';
End     : 'end';
Read    : 'read';
Write   : 'write';
Return  : 'return';
Func    : 'func';

If : 'if';
Else : 'else';
While : 'while';



Constant  : ('0x' | '0b') ? Digit+ ('.' Digit+)?;
Identifier  : Letter+ Digit*;

Letter : 'a'..'z' | 'A'..'Z';
Digit : '0'..'9';


Comma : ',';
RightParen : ')';
LeftParen : '(';

Assign : ':=';



Compare : (Equals | NotEquals | Grater | GraterEquals | Lower | LowerEquals);

Equals : '=';
NotEquals : '!=';
Grater : '>';
GraterEquals : '>=';
Lower : '<';
LowerEquals : '<=';

LogicalOperator : And | Or;

And : '&';
Or  : '|';

Operator : DivOperator | ProdOperator | AddOpetator | SubOperator;

AddOpetator  : '+';
SubOperator  : '-';
ProdOperator : '*';
DivOperator  : '/';


Semicolon : ';';

WhiteSpace : (' '|'\t') -> skip;
NewLine : ('\r''\n'|'\r'|'\n') -> skip;
BlockComment:   '/*' .*? '*/' -> skip;
LineComment:   '//' ~[\r\n]*  -> skip;


// Syntactic Grammar

methodCall : Identifier LeftParen listOfExpression? RightParen;
identifierCall : Identifier;

primary : identifierCall | Constant | LeftParen expression RightParen | methodCall;
rightPrimary : Operator primary;
expression : primary rightPrimary*;

listOfIdentifier : Identifier (Comma Identifier)*;
listOfArguments  : Identifier (Comma Identifier)*;
listOfExpression : expression (Comma expression)*;
listOfStatement  : statement* returnStatement?;

readStatement : Read LeftParen listOfIdentifier RightParen Semicolon;
assignStatement : (Identifier Assign)? expression  Semicolon;
writeStatement : Write LeftParen listOfExpression RightParen  Semicolon;
returnStatement : Return expression Semicolon;

comparison : expression Compare expression;
rightComparison : LogicalOperator comparison;
elseSentences : Else listOfStatement;

ifStatement : If LeftParen comparison rightComparison* RightParen listOfStatement (elseSentences)? End;
//whileStatement : While LeftParen comparison rightComparison* RightParen listOfStatement End;

statement :  (readStatement | assignStatement | writeStatement | ifStatement/*| whileStatement*/);

method : Func Identifier (LeftParen listOfArguments RightParen)? listOfStatement End;

listOfMethod : method*;

program : listOfMethod Begin listOfStatement End listOfMethod EOF;
