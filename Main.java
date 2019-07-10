import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;
import java.util.*;


public class Main{
	static class Type{
		String name;
		int array;
		Type(emxstarParser.TypeContext ttx){
			name = ttx.getChild(0).getText();
			array = ttx.getChildCount() / 2;
//			System.out.printf("type " + name + " %d\n", array);
		}
		Type(String o){
			name = o;
		}
		boolean equal(Type tmp){
			if(name.equals("void")!= tmp.name.equals("void"))
				return false;
			return tmp.name.equals(name)&&tmp.array == array||
				tmp.name == "null"&&(array!= 0||!name.equals("int")&&!name.equals("bool")&&!name.equals("string"))||
				name == "null"&&(tmp.array!= 0||!tmp.name.equals("int")&&!tmp.name.equals("bool")&&!tmp.name.equals("string"));
		}
	}
	static class Node{
		String kind;

		String id;
		List <Node> ch;

		Type type;
		List <Type> tys;
		List <String> ids;
		Map<String,Def>scope;

		boolean isscope;

		boolean leftvalue;
		List<Code>ir;
		int ans;
		boolean ismem;

		Node(){
			tys = new ArrayList<Type>();
			ids = new ArrayList<String>();
			ch = new ArrayList<Node>();
			scope = new TreeMap<String,Def>();
			ir = new ArrayList<Code>();
		}
}
	static Node visit(ParseTree now)throws Exception{
		Node tmp = new Node();
		if(now instanceof emxstarParser.ProgramContext){
//				System.out.printf("PROGRAM\n");
			emxstarParser.ProgramContext cnt = (emxstarParser.ProgramContext)now;
			tmp.kind = "program";
			tmp.isscope = true;
//				System.out.printf("%d\n",cnt.getChildCount());
			for(ParseTree i: cnt.children){
				tmp.ch.add(visit(i));
			}
//				System.out.printf("Finish PROGRAM\n");
		}
		else if(now instanceof emxstarParser.FuncContext){
//				System.out.printf("FUNC\n");
			emxstarParser.FuncContext cnt = (emxstarParser.FuncContext)now;
			tmp.kind = "func";
			tmp.isscope = true;
			tmp.id = cnt.Id(0).getText();
//				System.out.printf("%d\n",cnt.type().size());
			tmp.type = new Type(cnt.type(0));
//				System.out.printf("FUNC2\n");
//				System.out.printf("!!!!!!!!!!!!!%d",cnt.type().size());
			for(int i = 1; i < cnt.type().size(); i++){
				tmp.tys.add(new Type(cnt.type(i)));
				tmp.ids.add(cnt.Id(i).getText());
			}
//				System.out.printf("FUNC3\n");
			tmp.ch.add(visit(cnt.block()));
//				System.out.printf("Finish FUNC\n");
		}
		else if(now instanceof emxstarParser.FuncconContext){
			emxstarParser.FuncconContext cnt = (emxstarParser.FuncconContext)now;
			tmp.kind = "funccon";
			tmp.isscope = true;
			tmp.id = cnt.Id().getText();
			tmp.ch.add(visit(cnt.block()));
		}
		else if(now instanceof emxstarParser.VariContext){
//				System.out.printf("VARI\n");
			emxstarParser.VariContext cnt = (emxstarParser.VariContext)now;
			tmp.kind = "vari";
			tmp.type = new Type(cnt.type());
			tmp.id = cnt.Id().getText();
//				System.out.printf("VARI "+" " + tmp.type.name +" %d " +tmp.id+"\n",tmp.type.array);
			if (cnt.getChildCount()  ==  5)
				tmp.ch.add(visit(cnt.getChild(3))); 
//				System.out.printf("Finish VARI\n");
		}
		else if(now instanceof emxstarParser.ClasContext){
			emxstarParser.ClasContext cnt = (emxstarParser.ClasContext)now;
			tmp.kind = "clas";
			tmp.isscope = true;
			tmp.id = cnt.Id().getText();
			for(int i = 3; i + 1 < cnt.getChildCount(); i++)
				tmp.ch.add(visit(cnt.getChild(i)));
		}
		else if(now instanceof emxstarParser.BlockContext){
//				System.out.printf("BLOCK\n");
			emxstarParser.BlockContext cnt = (emxstarParser.BlockContext)now;
			tmp.kind = "block";
			tmp.isscope = true;
//				System.out.printf("%d\n",cnt.getChildCount() - 2);
			for(int i = 1; i + 1 < cnt.getChildCount(); i++)
				tmp.ch.add(visit(cnt.getChild(i)));
//				System.out.printf("Finish BLOCK\n");
		}
		else if(now instanceof emxstarParser.StatementContext){
			emxstarParser.StatementContext cnt = (emxstarParser.StatementContext) now;
			tmp.kind = "statement";
			if (now instanceof emxstarParser.S2Context){
				tmp.kind = "empty";
				if (!cnt.getChild(0).getText().equals(";")){
					tmp.ch.add(visit(cnt.getChild(0)));
				}
			}
			if (now instanceof emxstarParser.S7Context){
				tmp = visit(cnt.getChild(0));
			}
			else if (now instanceof emxstarParser.S1Context){
				tmp.isscope = true;
				tmp.kind = "block";
				tmp.ch.add(visit(cnt.getChild(0)));
			}
			else if (now instanceof emxstarParser.S6Context){
//					System.out.printf("S6\n");
				tmp.kind = cnt.getChild(0).getText();
//					System.out.printf(tmp.kind+"\n");
//					System.out.printf("%d\n",cnt.getChildCount());
				if (cnt.getChildCount()  ==  3)
					tmp.ch.add(visit(cnt.getChild(1)));
//					System.out.printf("Finish S6\n");
			}
			else if (now instanceof emxstarParser.S3Context){
				tmp.kind = "if";
				tmp.ch.add(visit(cnt.getChild(2)));
				tmp.ch.add(visit(cnt.getChild(4)));
				if (cnt.getChildCount()  ==  7) 
					tmp.ch.add(visit(cnt.getChild(6)));
				for (int i = 1; i < tmp.ch.size(); i++){
					if (tmp.ch.get(i).kind  ==  "vari")
						tmp.ch.get(i).isscope = true;
				}
			}
			else if (now instanceof emxstarParser.S4Context){
				tmp.kind = "while";
				tmp.ch.add(visit(cnt.getChild(2)));
				tmp.ch.add(visit(cnt.getChild(4)));
				if (tmp.ch.get(1).kind  ==  "vari")
					tmp.ch.get(1).isscope = true;
			}
			else if (now instanceof emxstarParser.S5Context){
				tmp.kind = "for";
				int p = 2;
				if (!cnt.getChild(p).getText().equals(";")){
					tmp.ch.add(visit(cnt.getChild(p)));
					p++;
				} else tmp.ch.add(null);
				p++;
				if (!cnt.getChild(p).getText().equals(";")){
					tmp.ch.add(visit(cnt.getChild(p)));
					p++;
				} else tmp.ch.add(null);
				p++;
				if (!cnt.getChild(p).getText().equals(")")){
					tmp.ch.add(visit(cnt.getChild(p)));
					p++;
				} else tmp.ch.add(null);
				p++;
				tmp.ch.add(visit(cnt.getChild(p)));
				if (tmp.ch.get(3).kind  ==  "vari")
					tmp.ch.get(3).isscope = true;
			}
		}
		else if(now instanceof emxstarParser.ConstantContext){
			emxstarParser.ConstantContext cnt = (emxstarParser.ConstantContext)now;
			tmp.kind = "constant";
			tmp.id = now.getText();
		}
		else if(now instanceof emxstarParser.NameContext){
			emxstarParser.NameContext cnt = (emxstarParser.NameContext)now;
			tmp.kind = "name";
			tmp.id = now.getText();
		}
		else if(now instanceof emxstarParser.ExpressionContext){
			tmp.kind = "expr";
			if (now instanceof emxstarParser.E1Context){
				emxstarParser.E1Context cnt = (emxstarParser.E1Context)now;
				String o=now.getChild(1).getText();
				if(o.equals("++"))tmp.id="x++";
				if(o.equals("--"))tmp.id="x--";
				tmp.ch.add(visit(cnt.getChild(0)));
			}
			else if (now instanceof emxstarParser.E2Context){
				emxstarParser.E2Context cnt = (emxstarParser.E2Context)now;
				tmp.id = ".";
				tmp.ch.add(visit(cnt.expression()));
				tmp.ids.add(cnt.Id().getText());
			}
			else if (now instanceof emxstarParser.E3Context){
				emxstarParser.E3Context cnt = (emxstarParser.E3Context)now;
				tmp.id = "[]";
				tmp.ch.add(visit(cnt.getChild(0)));
				tmp.ch.add(visit(cnt.getChild(2)));
			}
			else if (now instanceof emxstarParser.E4Context){
				emxstarParser.E4Context cnt = (emxstarParser.E4Context)now;
				tmp.id = "func";
//					System.out.printf("E4\n");
//					System.out.printf("E4 %d\n",cnt.getChildCount());
				for(int i = 0; i < cnt.getChildCount() - 1; i += 2)
					tmp.ch.add(visit(cnt.getChild(i)));
//					System.out.printf("E4 %d\n",tmp.ch.size());
			}
			else if (now instanceof emxstarParser.E5Context){
				emxstarParser.E5Context cnt = (emxstarParser.E5Context)now;
				String o = cnt.getChild(0).getText();
				if(o.equals("++"))tmp.id="++";
				if(o.equals("--"))tmp.id="--";
				if(o.equals("~"))tmp.id="~";
				if(o.equals("!"))tmp.id="!";
				if(o.equals("-"))tmp.id="-";
				if(o.equals("+"))tmp.id="+";
				tmp.ch.add(visit(cnt.getChild(1)));
			}
			else if (now instanceof emxstarParser.E6Context){
				emxstarParser.E6Context cnt = (emxstarParser.E6Context)now;
				tmp.id = "new";
//					System.out.printf(cnt.getChild(1).getText());
				tmp.ids.add(cnt.getChild(1).getText());
				int flag = 1;
				for (int i = 3; i < cnt.getChildCount(); i  +=  2){
					if(cnt.getChild(i).getText().equals(")"));
					else if (!cnt.getChild(i).getText().equals("]")){
						if (flag == 0)  throw new Exception();
						tmp.ch.add(visit(cnt.getChild(i)));
						i++;
					}
					else{
						tmp.ch.add(null);
						flag = 0;
					}
				}
			}
			else if (now instanceof emxstarParser.E7Context){
				emxstarParser.E7Context cnt = (emxstarParser.E7Context)now;
				String o=now.getChild(1).getText();
				if(o.equals("*"))tmp.id="*";
				if(o.equals("/"))tmp.id="/";
				if(o.equals("%"))tmp.id="%";
				if(o.equals("+"))tmp.id="+";
				if(o.equals("-"))tmp.id="-";
				if(o.equals("<<"))tmp.id="<<";
				if(o.equals(">>"))tmp.id=">>";
				if(o.equals("<"))tmp.id="<";
				if(o.equals(">"))tmp.id=">";
				if(o.equals("<="))tmp.id="<=";
				if(o.equals(">="))tmp.id=">=";
				if(o.equals("=="))tmp.id="==";
				if(o.equals("!="))tmp.id="!=";
				if(o.equals("&"))tmp.id="&";
				if(o.equals("^"))tmp.id="^";
				if(o.equals("|"))tmp.id="|";
				if(o.equals("&&"))tmp.id="&&";
				if(o.equals("||"))tmp.id="||";
				if(o.equals("="))tmp.id="=";
				tmp.ch.add(visit(cnt.getChild(0)));
				tmp.ch.add(visit(cnt.getChild(2)));
			}
			else if (now instanceof emxstarParser.E8Context){
//					System.out.printf("IDCON\n");
				emxstarParser.E8Context cnt = (emxstarParser.E8Context)now;
				tmp.id = "idcon";
				tmp.ch.add(visit(cnt.getChild(0)));
//					System.out.printf("finish IDCON\n");
			}
			else if (now instanceof emxstarParser.E9Context){
				tmp.id = "()";
				emxstarParser.E9Context cnt = (emxstarParser.E9Context)now;
				tmp.ch.add(visit(cnt.getChild(1)));
			}
		}
		else throw new Exception();
		return tmp;
	}
	static class Def{
		String kind;
		Type type;
		Map<String,Def>defs;
		List<Type>para;
		int id;
		Def(){
			defs = new TreeMap<String,Def>();
			para = new ArrayList<Type>();
		}
	}
	static class Code{
		String op;
		int c,a,b;
		Code(String o,int cc,int aa,int bb){
			op = o;c = cc;a = aa;b = bb;
		}
	}
	static List<Node>ancestor = new ArrayList<Node>();
	static int funct,vart = 16,labt,globalvart,conststringt;
	static boolean addr;
	static Map<Integer,String>vkind = new TreeMap<Integer,String>();
	static Map vnum = new TreeMap();
	static List<String>conststr = new ArrayList<String>();
	static int lastregister;
	static Random rand = new Random();
	static void check(Node now)throws Exception{
		ancestor.add(now);
//		if (now.isscope) System.out.printf(now.kind+" isscope\n");
//		else System.out.printf(now.kind+" !isscope\n");
		if(now.kind.equals("program")){
//			System.out.printf("program\n");
			Def d = new Def(), f;
			d.kind = "class";
			now.scope.put("int",d);
			d = new Def();
			d.kind = "class";
			now.scope.put("bool",d);
			
			d = new Def();
			d.kind = "class";
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("int");
			f.id = ++funct;
			d.defs.put("length",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("string");
			f.para.add(new Type("int"));
			f.para.add(new Type("int"));
			f.id = ++funct;
			d.defs.put("substring",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("int");
			f.id = ++funct;
			d.defs.put("parseInt",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("int");
			f.para.add(new Type("int"));
			f.id = ++funct;
			d.defs.put("ord",f);
			now.scope.put("string",d);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("void");
			f.para.add(new Type("string"));
			f.id = ++funct;
			now.scope.put("print",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("void");
			f.para.add(new Type("string"));
			f.id = ++funct;
			now.scope.put("println",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("string");
			f.id = ++funct;
			now.scope.put("getString",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("int");
			f.id = ++funct;
			now.scope.put("getInt",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("string");
			f.para.add(new Type("int"));
			f.id = ++funct;
			now.scope.put("toString",f);
			
			f = new Def();
			f.kind = "func";
			f.type = new Type("int");
			f.id = ++funct;
			now.scope.put("size!",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string+",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string<",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string>",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string<=",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string>=",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string==",f);
			
			f = new Def();
			f.kind = "func";
			f.id = ++funct;
			now.scope.put("string!=",f);
			
			for (Node i:now.ch){
				if (i.kind.equals("clas")){
					if(now.scope.get(i.id) != null)
						throw new Exception();
					Def cdef = new Def();
					cdef.kind = "class";
					int classvart = 0;
					boolean hasconstruct = false;
					for(Node j:i.ch){
						if (j.kind.equals("func")){
							if (cdef.defs.get(j.id) != null)
								throw new Exception();
							Def fdef = new Def();
							fdef.kind = "func";
							fdef.type = j.type;
							for (Type k: j.tys)
								fdef.para.add(k);
							fdef.id = ++funct;
							cdef.defs.put(j.id,fdef);
							i.scope.put(j.id,fdef);
							j.ir.add(new Code("funclab",fdef.id,0,0));
						}
						else if (j.kind.equals("vari")){
							if (cdef.defs.get(j.id) !=  null)
								throw new Exception();
							Def vdef = new Def();
							vdef.kind = "var";
							vdef.type = j.type;
							vdef.id=++classvart;
							cdef.defs.put(j.id, vdef);
							i.scope.put(j.id, vdef);
						}
						else if(j.kind.equals("funccon")){
							hasconstruct = true;
							if(!j.id.equals(i.id))
								throw new Exception();
							if(cdef.defs.get(j.id) != null)
								throw new Exception();
							Def fdef = new Def();
							fdef.kind = "func";
							fdef.id = ++funct;
							cdef.defs.put(j.id, fdef);
							i.scope.put(j.id, fdef);
							j.ir.add(new Code("funclab",fdef.id,0,0));
							j.ir.add(1,new Code("get",j.ans = ++vart,0,0));
						}
					}
					if(!hasconstruct){
						Node j = new Node();
						j.kind = "funccon";
						j.id = i.id;
						if(cdef.defs.get(j.id)!= null)
							throw new Exception();
						i.ch.add(j);
						Def fdef = new Def();
						fdef.kind = "func";
						fdef.id = ++funct;
						cdef.defs.put(j.id, fdef);
						i.scope.put(j.id, fdef);
						j.ir.add(new Code("funclab", fdef.id, 0, 0));
						j.ir.add(1, new Code("get",j.ans = ++vart, 0, 0));
					}
					cdef.id = classvart;
					now.scope.put(i.id, cdef);
				}
				else if (i.kind.equals("func")){
					if (now.scope.get(i.id) !=  null)
						throw new Exception();
					Def fdef = new Def();
					fdef.kind = "func";
					fdef.type = i.type;
					for (Type k : i.tys)
						fdef.para.add(k);
					fdef.id = ++funct;
					now.scope.put(i.id, fdef);
					i.ir.add(new Code("funclab",fdef.id,0,0));
				}
			}
			boolean havemain = false;
			for(Node i: now.ch)
				if(i.kind == "func" && i.id.equals("main")
				&& i.type.equal(new Type("int")) && i.tys.size() == 0)
					havemain = true;
			if(!havemain)throw new Exception();
			Node j = new Node();
			j.kind = "func";
			now.ch.add(j);
			j.ir.add(new Code("funclab",0,0,0));
			for(Node i:now.ch)
				if(i!= j){
					check(i);
					now.ir.addAll(i.ir);
				}
			j.ir.add(new Code("call",now.scope.get("main").id,0,0));
			j.ir.add(new Code("ret",0,0,0));
			now.ir.addAll(j.ir);
		}
		else if (now.kind.equals("clas")){
			Node j = null;
			for (Node i: now.ch)
				if(i.kind.equals("funccon"))
					j = i;
				else {
					check(i);
					now.ir.addAll(i.ir);
					if(i.kind.equals("func") && i.id.equals(now.id))
						throw new Exception();
				}
			check(j);
			now.ir.addAll(j.ir);
		}
		else if (now.kind.equals("func")){
			if(!now.type.equal(new Type("void")) && (
			ancestor.get(0).scope.get(now.type.name)  ==  null ||
			!ancestor.get(0).scope.get(now.type.name).kind.equals("class")))
					throw new Exception();
			if(ancestor.get(1).kind == "clas")
				now.ir.add(new Code("get",now.ans = ++vart,0,0));
			for (int i = 0; i < now.tys.size(); i++){
				if(ancestor.get(0).scope.get(now.tys.get(i).name)  ==  null||
					!ancestor.get(0).scope.get(now.tys.get(i).name).kind.equals("class"))
						throw new Exception();
				Def vdef = new Def();
				vdef.kind = "var";
				vdef.type = now.tys.get(i);
				vdef.id = ++vart;
				now.scope.put(now.ids.get(i),vdef);
				now.ir.add(new Code("get",vdef.id,0,0));
			}
			check(now.ch.get(0));
			now.ir.addAll(now.ch.get(0).ir);
			now.ir.add(new Code("ret",0,0,0));
			now.ir.add(new Code("",0,0,0));
//			System.out.printf("finish func\n");
		}
		else if (now.kind.equals("funccon")){
			for(Node i: now.ch){
				check(i);
				now.ir.addAll(i.ir);
			}
			now.ir.add(new Code("ret",0,0,0));
			now.ir.add(new Code("",0,0,0));
		}
		else if (now.kind.equals("vari")){
			if(ancestor.get(0).scope.get(now.type.name)  ==  null||
				!ancestor.get(0).scope.get(now.type.name).kind.equals("class"))
					throw new Exception();
			Node fa = null;
			for(Node i: ancestor)
				if(i.isscope)
					fa = i;
			if (now.ch.size()  ==  1){
				check(now.ch.get(0));
				if(!now.type.equal(now.ch.get(0).type))
					throw new Exception();
			}
			if(!fa.kind.equals("clas")){
				if(fa.scope.get(now.id)!= null)
					throw new Exception();
				Def vdef = new Def();
				vdef.kind = "var";
				vdef.type = now.type;
				vdef.id = ++vart;
				if(fa.kind == "program"){
					vkind.put(vdef.id,"global");
					vnum.put(vdef.id,8*(++globalvart));
				}
				fa.scope.put(now.id, vdef);
			}
			if(now.ch.size() != 0)
				if(fa.kind.equals("clas")){
					Node z = null;
					for(Node ii:fa.ch)
						if(ii.type == null)
							z = ii;
					int v1 = ++vart,v2 = ++vart;
					vkind.put(v1,"const");
					vnum.put(v1,8*fa.scope.get(now.id).id);
					z.ir.addAll(now.ch.get(0).ir);
					z.ir.add(new Code("+",v2,z.ans,v1));
					z.ir.add(new Code("save",0,v2,now.ch.get(0).ans));
				}
				else if(fa.kind == "program"){
					Node z = fa.ch.get(fa.ch.size()-1);
					z.ir.addAll(now.ch.get(0).ir);
					z.ir.add(new Code("mov",fa.scope.get(now.id).id,now.ch.get(0).ans,0));
				}
				else{
					now.ir.addAll(now.ch.get(0).ir);
					now.ir.add(new Code("mov",fa.scope.get(now.id).id,now.ch.get(0).ans,0));
				}
//			System.out.printf("finish var\n");
		}
		else if (now.kind.equals("block")){
//			System.out.printf("block\n");
			for (Node i: now.ch){
				check(i);
				now.ir.addAll(i.ir);
			}
//			System.out.printf("finish block\n");
		}
		else if(now.kind.equals("empty"))
			for (Node i: now.ch){
				check(i);
				now.ir.addAll(i.ir);
			}
		else if(now.kind.equals("break") || now.kind.equals("continue")){
			Node fa = null;
			for(Node i:ancestor)
				if(i.kind.equals("while") || i.kind.equals("for"))
					fa = i;
			if(fa  ==  null)throw new Exception();
			now.ir.add(new Code("jmp",now.kind.equals("break")?fa.ans+1:fa.ans+3,0,0));
		}
		else if(now.kind.equals("return")){
			Node fa = null;
			for(Node i: ancestor)
				if(i.kind.equals("func") || i.kind.equals("funccon"))
					fa = i;
			if(fa==null)throw new Exception();
			if(fa.type.equal(new Type("void"))){
				if(now.ch.size() !=  0)
					throw new Exception();
				now.ir.add(new Code("ret",0,0,0));
			}
			else{
				if(now.ch.size() == 0)
					throw new Exception();
				check(now.ch.get(0));
				if(!fa.type.equal(now.ch.get(0).type))
					throw new Exception();
				now.ir.addAll(now.ch.get(0).ir);
				now.ir.add(new Code("mov",1,now.ch.get(0).ans,0));
				now.ir.add(new Code("ret",0,0,0));	
			}
		}
		else if(now.kind.equals("if")){
			int lab = labt;
			labt += 2;
			now.ans = lab;
			check(now.ch.get(0));
			if(!now.ch.get(0).type.equal(new Type("bool")))
				throw new Exception();
			for (int i = 1; i < now.ch.size(); i++)
				check(now.ch.get(i));
			now.ir.addAll(now.ch.get(0).ir);
			now.ir.add(new Code("test",0,now.ch.get(0).ans,0));
			now.ir.add(new Code("jnz",lab+1,0,0));
			if(now.ch.size()  ==  3)
				now.ir.addAll(now.ch.get(2).ir);
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
			now.ir.addAll(now.ch.get(1).ir);
			now.ir.add(new Code("label",lab+2,0,0));
		}
		else if(now.kind.equals("while")){
			int lab = labt;
			labt += 3;
			now.ans = lab;
			check(now.ch.get(0));
			if(!now.ch.get(0).type.equal(new Type("bool")))
				throw new Exception();
			check(now.ch.get(1));
			now.ir.add(new Code("label",lab+2,0,0));
			now.ir.addAll(now.ch.get(0).ir);
			now.ir.add(new Code("test",0,now.ch.get(0).ans,0));
			now.ir.add(new Code("jz",lab+1,0,0));
			now.ir.addAll(now.ch.get(1).ir);
			now.ir.add(new Code("label",lab+3,0,0));
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
		}
		else if(now.kind.equals("for")){
			int lab = labt;
			labt += 3;
			now.ans = lab;
			for (int i = 0; i  <=  3; i++)
				if (now.ch.get(i) !=  null)
					check(now.ch.get(i));
			if (now.ch.get(1) !=  null)
				if(!now.ch.get(1).type.equal(new Type("bool")))
					throw new Exception();
			if(now.ch.get(0)!= null)
				now.ir.addAll(now.ch.get(0).ir);
			now.ir.add(new Code("label",lab+2,0,0));
			if(now.ch.get(1)!= null){
				now.ir.addAll(now.ch.get(1).ir);
				now.ir.add(new Code("test",0,now.ch.get(1).ans,0));
				now.ir.add(new Code("jz",lab+1,0,0));
			}
			now.ir.addAll(now.ch.get(3).ir);
			now.ir.add(new Code("label",lab+3,0,0));
			if(now.ch.get(2)!= null)
				now.ir.addAll(now.ch.get(2).ir);
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
		}
		else if (now.kind.equals("constant")){
			if (now.id.equals("this")){
				if (ancestor.get(1).kind.equals("clas"))
					now.type = new Type(ancestor.get(1).id);
				else throw new Exception();
				now.ans = ancestor.get(2).ans;
			}
			else if(now.id.charAt(0) >= '0' && now.id.charAt(0) <= '9'){
				now.type = new Type("int");
				now.ans = ++vart;
//				System.out.printf(now.id+"\n");
//				System.out.printf("!!%d\n\n",now.ans);
				vkind.put(now.ans,"const");
				vnum.put(now.ans,Integer.valueOf(now.id));
//				System.out.printf("!!%d\n\n",now.ans);
				now.ir.add(new Code("mov",++vart,now.ans,0));
//				System.out.printf("!!%d\n\n",vart);
				now.ans = vart;
			}
			else if(now.id.equals("true")){
				now.type = new Type("bool");
				now.ans = ++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,1);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans = vart;
			}
			else if(now.id.equals("false")){
				now.type = new Type("bool");
				now.ans = ++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,0);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans = vart;
			}
			else if(now.id.charAt(0) == '"'){
				now.type = new Type("string");
				now.ans = ++vart;
				conststr.add(now.id);
				vkind.put(now.ans,"string");
				vnum.put(now.ans,++conststringt);
			}
			else if(now.id.equals("null")){
				now.type = new Type("null");
				now.ans = ++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,0);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans = vart;
			}
		}
		else if (now.kind.equals("name")){
//			System.out.printf(now.id + "\n");
			Node fr = null;
			Def d = null;
			for(Node i: ancestor)
				if(i.scope.get(now.id) != null){
					fr = i;
					d = i.scope.get(now.id);
				}
			if(d == null || d.kind.equals("class"))
				throw new Exception();
			if(d.kind.equals("var")){
				now.type = d.type;
				now.leftvalue = true;
			} else {
				now.type = new Type("*");
				now.tys.add(d.type);
				now.tys.addAll(d.para);
			}
			if(d.kind.equals("var") && fr.kind.equals("clas")){
				Node f = null;
				for(Node i: ancestor)
					if(i.kind.equals("func") || i.kind.equals("funccon"))
						f = i;
				int v1 = ++vart,v2 = ++vart;
				vkind.put(v1,"const");
				vnum.put(v1,8*d.id);
				now.ir.add(new Code("+",v2,f.ans,v1));
				now.ans = v2;
				if(!addr){
					now.ir.add(new Code("load",++vart,now.ans,0));
					now.ans = vart;
				}
				else now.ismem = true;
			}
			else now.ans = d.id;
		}
		else if(now.kind.equals("expr")){
//			System.out.printf("expr\n");
			if (now.id.equals(".")){
				boolean paddr=addr;
				addr=false;
				check(now.ch.get(0));
				addr=paddr;
				Type type = now.ch.get(0).type;
				if(type.array != 0){
					if(!now.ids.get(0).equals("size"))
						throw new Exception();
					now.type = new Type("*");
					now.tys.add(new Type("int"));
					Def f=ancestor.get(0).scope.get("size!");
					now.ans=f.id;
				}
				else{
					Def d = ancestor.get(0).scope.get(type.name);
					if(d == null || d.defs.get(now.ids.get(0)) == null)
						throw new Exception();
					d = d.defs.get(now.ids.get(0));
					if(d.kind.equals("func")){
						now.type = new Type("*");
						now.tys.add(d.type);
						now.tys.addAll(d.para);
						now.ans=d.id;
					}
					else{
						now.type = d.type;
						now.leftvalue = true;
						now.ir.addAll(now.ch.get(0).ir);
						int v1 = ++vart,v2 = ++vart;
						vkind.put(v1,"const");
						vnum.put(v1,8*d.id);
						now.ir.add(new Code("+",v2,now.ch.get(0).ans,v1));
						now.ans = v2;
						if(!addr){
							now.ir.add(new Code("load",++vart,now.ans,0));
							now.ans = vart;
						}
						else now.ismem = true;
					}
				}
			}
			else if (now.id.equals("func")){
				for (Node i: now.ch){
					check(i);
				}
				Node f = now.ch.get(0);
//				System.out.printf(f.type.name+"\n");
//				System.out.printf("%d %d\n",f.tys.size(), now.ch.size());
				if(!f.type.name.equals("*") || f.tys.size() !=  now.ch.size())
					throw new Exception();
				for(int i = 1; i < f.tys.size(); i++)
					if(!f.tys.get(i).equal(now.ch.get(i).type))
						throw new Exception();
				now.type = f.tys.get(0);
				if(f.id.equals("."))
					now.ir.addAll(f.ch.get(0).ir);
				for(int i = 1;i<now.ch.size();i++)
					now.ir.addAll(now.ch.get(i).ir);
				if(f.id.equals("."))
					now.ir.add(new Code("send",0,f.ch.get(0).ans,0));
				else if(ancestor.get(1).kind == "clas"&&ancestor.get(1).scope.get(f.id)!= null)
					now.ir.add(new Code("send",0,ancestor.get(2).ans,0));
				for(int i = 1;i<now.ch.size();i++)
					now.ir.add(new Code("send",0,now.ch.get(i).ans,0));
				now.ir.add(new Code("call",f.ans,0,0));
				now.ir.add(new Code("mov",++vart,1,0));
				now.ans = vart;
			}
			else if(now.id.equals("[]")){
				boolean paddr = addr;
				if(addr == true)
					addr = false;
				for(Node i: now.ch)
					check(i);
				addr = paddr;
				if(now.ch.get(0).type.array == 0||
					!now.ch.get(1).type.equal(new Type("int")))
						throw new Exception();
				now.type = new Type(now.ch.get(0).type.name);
				now.type.array = now.ch.get(0).type.array - 1;
				now.leftvalue = true;
				now.ir.addAll(now.ch.get(0).ir);
				now.ir.addAll(now.ch.get(1).ir);
				now.ans = ++vart;
				now.ir.add(new Code("lea",now.ans,now.ch.get(0).ans,now.ch.get(1).ans));
				if(!addr){
					now.ir.add(new Code("load",++vart,now.ans,0));
					now.ans = vart;
				}
				else now.ismem = true;
			}
			else if(now.id.equals("new")){
				Def d = ancestor.get(0).scope.get(now.ids.get(0));
				if(d == null || !d.kind.equals("class"))throw new Exception();
				boolean empty = false;
				int num = 0;
				for(Node i: now.ch){
					if(i != null){
						num++;
						if (empty) throw new Exception();
						check(i);
						if(!i.type.equal(new Type("int")))
							throw new Exception();
						now.ir.addAll(i.ir);
					}
					else empty=true;
				}
				now.type = new Type (now.ids.get(0));
				now.type.array = now.ch.size();
				if(num!= 0){
					for(int i = 1;i <= num;i++)
						now.ir.add(new Code("mov",vart+i,now.ch.get(i-1).ans,0));
					vart += num;
					int v0 = vart-num, ir0 = now.ir.size(), vv0 = vart+1;
					vart += num;
					for(int i = num;i!= 0;i--){
						int v1 = ++vart,v2 = ++vart;
						vkind.put(v2,"const");
						vnum.put(v2,0);
						now.ir.add(ir0,new Code("malloc",vv0+i,v0+i,0));
						if(i!= num){
							labt += 2; 
							now.ir.add(ir0+1,new Code("mov",v1,v2,0));
							now.ir.add(ir0+2,new Code("label",labt-1,0,0));
							now.ir.add(ir0+3,new Code("<",++vart,v1,v0+i));
							now.ir.add(ir0+4,new Code("test",0,vart,0));
							now.ir.add(ir0+5,new Code("jz",labt,0,0));
							now.ir.add(new Code("lea",++vart,vv0+i,v1));
							now.ir.add(new Code("save",0,vart,vv0+i+1));
							now.ir.add(new Code("++",v1,v1,0));
							now.ir.add(new Code("jmp",labt-1,0,0));
							now.ir.add(new Code("label",labt,0,0));
						}
					}
					now.ans = vv0+1;
				}
				else{
					int v1 = ++vart,v2 = ++vart;
					vkind.put(v2,"const");
					vnum.put(v2,8*d.id);
					now.ir.add(new Code("malloc",v1,v2,0));
					now.ir.add(new Code("send",0,v1,0));
					now.ir.add(new Code("call",d.defs.get(now.ids.get(0)).id,0,0));
					now.ans = v1;
				}
			}
			else if(now.ch.size() == 1){
				boolean paddr = addr;
				if(now.id.equals("++")||now.id.equals("--")||now.id.equals("x++")||now.id.equals("x--"))
					addr = true;
				check(now.ch.get(0));
				addr = paddr;
				Type type = now.ch.get(0).type;
				if(now.id.equals("x++") || now.id.equals("x--") || now.id.equals("++") || now.id.equals("--") || now.id.equals("~") || now.id.equals("-") || now.id.equals("+")){
					if(!type.equal(new Type("int")))
						throw new Exception();
				}
				else if(now.id.equals("!")){
					if(!type.equal(new Type("bool")))
						throw new Exception();
				}
				if(now.id.equals("++") || now.id.equals("--") || now.id.equals("x++") || now.id.equals("x--"))
					if(!now.ch.get(0).leftvalue)
						throw new Exception();
				if(now.id.equals("x++") || now.id.equals("x--"))
					now.leftvalue = true;
				now.type = type;
				if(now.id.equals("x++") || now.id.equals("x--")){
					now.ir.addAll(now.ch.get(0).ir);
					if(now.ch.get(0).ismem){
						int v1 = ++vart,v2 = ++vart;
						now.ir.add(new Code("load",v1,now.ch.get(0).ans,0));
						now.ir.add(new Code("mov",v2,v1,0));
						now.ir.add(new Code(now.id.equals("x++")?"++":"--",v1,v1,0));
						now.ir.add(new Code("save",0,now.ch.get(0).ans,v1));
						now.ans = v2;
					}
					else{
						int v1 = ++vart;
						now.ir.add(new Code("mov",v1,now.ch.get(0).ans,0));
						now.ir.add(new Code(now.id.equals("x++")?"++":"--",now.ch.get(0).ans,now.ch.get(0).ans,0));
						now.ans = v1;
					}
				}
				else if(now.id.equals("++")||now.id.equals("--")){
					now.ir.addAll(now.ch.get(0).ir);
					if(now.ch.get(0).ismem){
						int v1 = ++vart;
						now.ir.add(new Code("load",v1,now.ch.get(0).ans,0));
						now.ir.add(new Code(now.id,v1,v1,0));
						now.ir.add(new Code("save",0,now.ch.get(0).ans,v1));
						now.ans = now.ch.get(0).ans;
						now.ismem = true;
					}
					else{
						now.ir.add(new Code(now.id,now.ch.get(0).ans,now.ch.get(0).ans,0));
						now.ans = now.ch.get(0).ans;
					}
				}
				else if(now.id.equals("idcon")){
					now.leftvalue = now.ch.get(0).leftvalue;
					now.ir.addAll(now.ch.get(0).ir);
					now.ans = now.ch.get(0).ans;
					now.ismem = now.ch.get(0).ismem;
					if (now.ch.get(0).type.name.equals("*")){
						now.type = new Type("*");
						now.tys.addAll(now.ch.get(0).tys);
//						System.out.printf(now.id+".tys.size() = %d\n",now.tys.size());
					}
				}
				else if(now.id.equals("()")){
					now.leftvalue = now.ch.get(0).leftvalue;
					now.ir.addAll(now.ch.get(0).ir);
					now.ans = now.ch.get(0).ans;
					now.ismem = now.ch.get(0).ismem;
				}
				else{
					now.ir.addAll(now.ch.get(0).ir);
					if(!now.id.equals("+")){
						now.ir.add(new Code(now.id.equals("-")?"neg":now.id,++vart,now.ch.get(0).ans,0));
						now.ans = vart;
					}
					else now.ans = now.ch.get(0).ans;
				}
				if(now.ismem&&!addr){
					now.ir.add(new Code("load",++vart,now.ans,0));
					now.ans = vart;
				}
			}
			else{
				boolean paddr = addr;
				if(now.id.equals("="))
					addr = true;
				check(now.ch.get(0));
				addr = paddr;
				check(now.ch.get(1));
				Type t1 = now.ch.get(0).type,t2 = now.ch.get(1).type;
				if(now.id.equals("*") || now.id.equals("/") || now.id.equals("%") || now.id.equals("<<") || now.id.equals(">>") || now.id.equals("&") || now.id.equals("|") || now.id.equals("^") || now.id.equals("-")){
					if(!(t1.equal(t2) && t1.equal(new Type("int"))))
						throw new Exception();
					now.type = t1;
				}
				else if(now.id.equals("+") || now.id.equals(">") || now.id.equals("<") || now.id.equals(">=") || now.id.equals("<=")){
					if(!(t1.equal(t2) && (t1.equal(new Type("int")) || t1.equal(new Type("string")))))
						throw new Exception();
					now.type = (now.id.equals("+")) ? t1 : new Type("bool");
				}
				else if(now.id.equals("&&") || now.id.equals("||")){
					if(!(t1.equal(t2) && t1.equal(new Type("bool"))))
						throw new Exception();
					now.type = t1;
					
				}
				else if(now.id.equals("==") || now.id.equals("!=")){
					if(!t1.equal(t2))
						throw new Exception();
					now.type = new Type("bool");
				}
				else if(now.id.equals("=")){
					if(!t1.equal(t2) || now.ch.get(0).leftvalue  ==  false)
						throw new Exception();
					now.type = t1;
				}
				else throw new Exception();
				if(now.id.equals("&&") || now.id.equals("||")){
					int lab = labt,v = ++vart;
					labt += 2;
					now.ir.addAll(now.ch.get(0).ir);
					now.ir.add(new Code("test",0,now.ch.get(0).ans,0));
					now.ir.add(new Code("jnz",lab+1,0,0));
					if(now.id.equals("&&")){
						int vv = ++vart;
						vkind.put(vv,"const");
						vnum.put(vv,0);
						now.ir.add(new Code("mov",v,vv,0));
					}
					else{
						now.ir.addAll(now.ch.get(1).ir);
						now.ir.add(new Code("mov",v,now.ch.get(1).ans,0));
					}
					now.ir.add(new Code("jmp",lab+2,0,0));
					now.ir.add(new Code("label",lab+1,0,0));
					if(now.id.equals("||")){
						int vv = ++vart;
						vkind.put(vv,"const");
						vnum.put(vv,1);
						now.ir.add(new Code("mov",v,vv,0));
					}
					else{
						now.ir.addAll(now.ch.get(1).ir);
						now.ir.add(new Code("mov",v,now.ch.get(1).ans,0));
					}
					now.ir.add(new Code("label",lab+2,0,0));
					now.ans = v;
				}
				else if(now.id.equals("=")){
					now.ir.addAll(now.ch.get(0).ir);
					now.ir.addAll(now.ch.get(1).ir);
					if(now.ch.get(0).ismem){
						now.ir.add(new Code("save",0,now.ch.get(0).ans,now.ch.get(1).ans));
					}
					else now.ir.add(new Code("mov",now.ch.get(0).ans,now.ch.get(1).ans,0));
					now.ans = now.ch.get(0).ans;
					now.ismem = now.ch.get(0).ismem;
					if(now.ismem&&!addr){
						now.ir.add(new Code("load",++vart,now.ans,0));
						now.ans = vart;
					}
				}
				else if((now.id.equals("+") || now.id.equals(">") || now.id.equals("<") || now.id.equals(">=") || now.id.equals("<=") || now.id.equals("==") || now.id.equals("!="))
				&& now.ch.get(0).type.equal(new Type("string"))){
					now.ir.addAll(now.ch.get(0).ir);
					now.ir.addAll(now.ch.get(1).ir);
					now.ir.add(new Code("send",0,now.ch.get(0).ans,0));
					now.ir.add(new Code("send",0,now.ch.get(1).ans,0));
					now.ir.add(new Code("call",ancestor.get(0).scope.get("string"+now.id).id,0,0));
					now.ir.add(new Code("mov",++vart,1,0));
					now.ans = vart;
				}
				else{
					Node l = now.ch.get(0),r = now.ch.get(1);
					now.ir.addAll(l.ir);
					now.ir.addAll(r.ir);
					now.ir.add(new Code(now.id,++vart,l.ans,r.ans));
					now.ans = vart;
				}
			}                                                                                                                                                                                                                                                                                                                         
//			System.out.printf("finish expr\n");
		}
		ancestor.remove(ancestor.size() - 1);	
	}
	static String regname[]={"","rax","rbx","rcx","rdx","rbp","rsp","rdi","rsi","r8","r9","r10","r11","r12","r13","r14","r15"};
	static StringBuffer ans=new StringBuffer();
	static String varstring(int v)throws Exception{
		String k=vkind.get(v);
		int vv=(int)vnum.get(v);
		if(k=="const")return""+vv;
		else if(k=="global")return"qword[gbl+"+vv+"]";
		else if(k=="string")return"S"+vv;
		else if(k=="stack")return"qword[rsp+"+vv+"]";
		else if(k=="register")return regname[vv];
		else throw new Exception();
	}
	static int toregister(int now)throws Exception{
		if(vkind.get(now)=="global"||vkind.get(now)=="stack"){
			ans.append("\tmov "+regname[(lastregister^=rand.nextInt(3)+1)+1]+","+varstring(now)+"\n");
			now=lastregister+1;
		}
		return now;
	}
	static Set<Integer>[]occur;
	static void codegen(List<Code>ir)throws Exception{
		BufferedReader reader=new BufferedReader(new FileReader("./builtin1.txt"));
		for(String line;(line=reader.readLine())!=null;)
			ans.append(line+"\n");
		for(int i=0;i<ir.size();i++){
			Code o=ir.get(i);
			if(o.op.equals("")){
				ans.append("\n");
			}
			else if(o.op=="funclab"){
				if(o.c==0)ans.append("main:\n");
				else ans.append("F"+o.c+":\n");
				ans.append("\tpush rbp\n");
				ans.append("\tmov rbp,rsp\n");
				ans.append("\tsub rsp,"+o.a+"\n");
			}
			else if(o.op=="label"){
				ans.append("L"+o.c+":\n");
			}
			else if(o.op=="send"){
				int j=i;
				while(ir.get(j).op=="send")j++;
				for(int w=i;w<j;w++){
					int cc=toregister(ir.get(w).a);
					ans.append("\tmov "+(w<=i+1?regname[w-i+7]:"qword[arg+"+8*(w-i-1)+"]")+","+varstring(cc)+"\n");
				}
				i=j-1;
			}
			else if(o.op=="get"){
				int j=i;
				while(ir.get(j).op=="get")j++;
				for(int w=i;w<j;w++){
					int cc=vkind.get(ir.get(w).c)=="global"||vkind.get(ir.get(w).c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:ir.get(w).c;
					ans.append("\tmov "+varstring(cc)+","+(w<=i+1?regname[w-i+7]:"qword[arg+"+8*(w-i-1)+"]")+"\n");
					if(cc<=4)ans.append("\tmov "+varstring(ir.get(w).c)+","+regname[cc]+"\n");
				}
				i=j-1;
			}
			else if(o.op=="call"){
				int[]used=new int[17];
				if(o.c!=1&&o.c!=10){
					for(int v:occur[i])
						if(vkind.get(v)=="register")
							used[(int)vnum.get(v)]=1;
					for(int j=9;j<=16;j++)
						if(used[j]==1)ans.append("\tpush "+varstring(j)+"\n");
				}
				if(o.c==1){
					ans.append("\txor rax,rax\n");
					ans.append("\tmov al,byte[rdi]\n");
				}
				else if(o.c==10){
					ans.append("\tmov rax,qword[rdi]\n");
				}
				else if(o.c==5||o.c==6){
					ans.append("\tmov rsi,rdi\n");
					ans.append("\tinc rsi\n");
					ans.append("\tmov rdi,"+(o.c==5?"format\n":"formatln\n"));
					ans.append("\txor rax,rax\n");
					ans.append("\tcall printf\n");
				}
				else if(o.c<=17){
					String ss[]={"","length","substring","parseInt","ord","print","println","getString","getInt","toString","size!","concat","strls","strgt","strle","strge","streq","strne"};
					ans.append("\tcall "+ss[o.c]+"\n");
				}
				else ans.append("\tcall F"+o.c+"\n");
				if(o.c!=1&&o.c!=10)
					for(int j=16;j>=9;j--)
						if(used[j]==1)ans.append("\tpop "+varstring(j)+"\n");
			}
			else if(o.op=="ret"){
				ans.append("\tleave\n");
				ans.append("\tret\n");
			}
			else if(o.op=="save"){
				int cc=toregister(o.a),aa=toregister(o.b);
				ans.append("\tmov qword["+varstring(cc)+"],"+varstring(aa)+"\n");
			}
			else if(o.op=="load"){
				int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:o.c;
				int aa=toregister(o.a);
				ans.append("\tmov "+varstring(cc)+",qword["+varstring(aa)+"]\n");
				if(cc<=4)ans.append("\tmov "+varstring(o.c)+","+regname[cc]+"\n");
			}
			else if(o.op=="mov"){
				int cc=o.c,aa=o.a;
				if((vkind.get(cc)=="global"||vkind.get(cc)=="stack"))
					aa=toregister(aa);
				ans.append("\tmov "+varstring(cc)+","+varstring(aa)+"\n");
			}
			else if(o.op=="lea"){
				int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:o.c;
				int aa=toregister(o.a),bb=toregister(o.b);
				ans.append("\tlea "+varstring(cc)+",["+varstring(aa)+"+8*"+varstring(bb)+"+8]\n");
				if(cc<=4)ans.append("\tmov "+varstring(o.c)+","+regname[cc]+"\n");
			}
			else if(o.op=="malloc"){
				ans.append("\tmov rdi,"+varstring(o.a)+"\n");
				ans.append("\tshl rdi,3\n");
				ans.append("\tadd rdi,8\n");
				for(int j=8;j<=15;j++)
					ans.append("\tpush r"+j+"\n");
				ans.append("\tcall malloc\n");
				for(int j=15;j>=8;j--)
					ans.append("\tpop r"+j+"\n");
				ans.append("\tmov "+regname[(lastregister=rand.nextInt(3)+1)+1]+","+varstring(o.a)+"\n");
				ans.append("\tmov qword[rax],"+regname[lastregister+1]+"\n");
				ans.append("\tmov "+varstring(o.c)+",rax\n");
			}
			else if(o.op=="jmp"){
				ans.append("\tjmp L"+o.c+"\n");
			}
			else if(o.op=="jz"||o.op=="jnz"){
				ans.append("\t"+o.op+" L"+o.c+"\n");
			}
			else if(o.op=="test"){
				int cc=toregister(o.a);
				ans.append("\ttest "+varstring(cc)+","+varstring(cc)+"\n");
			}
			else{
				if(o.op.equals("+")||o.op.equals("-")||o.op.equals("&")||o.op.equals("|")||o.op.equals("^")){
					int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"||varstring(o.c).equals(varstring(o.a))||varstring(o.c).equals(varstring(o.b))?(lastregister^=rand.nextInt(3)+1)+1:o.c;
					ans.append("\tmov "+varstring(cc)+","+varstring(o.a)+"\n");
					if(o.op.equals("+"))ans.append("\tadd ");
					else if(o.op.equals("-"))ans.append("\tsub ");
					else if(o.op.equals("&"))ans.append("\tand ");
					else if(o.op.equals("|"))ans.append("\tor ");
					else if(o.op.equals("^"))ans.append("\txor ");
					else throw new Exception();
					ans.append(varstring(cc)+","+varstring(o.b)+"\n");
					if(cc<=4)ans.append("\tmov "+varstring(o.c)+","+regname[cc]+"\n");
				}
				else if(o.op.equals("<")||o.op.equals(">")||o.op.equals("<=")||o.op.equals(">=")||o.op.equals("==")||o.op.equals("!=")){
					int aa=o.a,bb=o.b;
					if(vkind.get(aa)=="global"||vkind.get(aa)=="stack")
						bb=toregister(bb);
					int zz=(lastregister^=rand.nextInt(3)+1)+1;
					ans.append("\txor "+regname[zz]+","+regname[zz]+"\n");
					ans.append("\tcmp "+varstring(aa)+","+varstring(bb)+"\n");
					if(o.op.equals("<"))ans.append("\tsetl ");
					else if(o.op.equals("<="))ans.append("\tsetle ");
					else if(o.op.equals(">"))ans.append("\tsetg ");
					else if(o.op.equals(">="))ans.append("\tsetge ");
					else if(o.op.equals("=="))ans.append("\tsete ");
					else if(o.op.equals("!="))ans.append("\tsetne ");
					else throw new Exception();
					ans.append(regname[zz].charAt(1)+"l\n");
					ans.append("\tmov "+varstring(o.c)+","+regname[zz]+"\n");
				}
				else if(o.op.equals("*")){
					ans.append("\tmov rax,"+varstring(o.a)+"\n");
					ans.append("\timul "+varstring(o.b)+"\n");
					ans.append("\tmov "+varstring(o.c)+",rax\n");
				}
				else if(o.op.equals("/")||o.op.equals("%")){
					ans.append("\tmov rdx,0\n");
					ans.append("\tmov rax,"+varstring(o.a)+"\n");
					ans.append("\tdiv "+varstring(o.b)+"\n");
					ans.append("\tmov "+varstring(o.c)+","+(o.op.equals("/")?"rax\n":"rdx\n"));
				}
				else if(o.op.equals("<<")||o.op.equals(">>")){
					int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"||varstring(o.c).equals(varstring(o.a))||varstring(o.c).equals(varstring(o.b))?(lastregister=2^(rand.nextInt(3)+1))+1:o.c;
					ans.append("\tmov "+varstring(cc)+","+varstring(o.a)+"\n");
					ans.append("\tmov rcx,"+varstring(o.b)+"\n");
					ans.append((o.op.equals("<<")?"\tshl ":"\tshr ")+varstring(cc)+",cl\n");
					if(cc<=4)ans.append("\tmov "+varstring(o.c)+","+regname[cc]+"\n");
				}
				else if(o.op.equals("++")||o.op.equals("--")||o.op.equals("neg")||o.op.equals("!")||o.op.equals("~")){
					int cc=toregister(o.c);
					if(!varstring(cc).equals(varstring(o.a)))
						ans.append("\tmov "+varstring(cc)+","+varstring(o.a)+"\n");
					if(o.op.equals("++"))ans.append("\tinc ");
					else if(o.op.equals("--"))ans.append("\tdec ");
					else if(o.op.equals("!"))ans.append("\txor "+varstring(cc)+",1\n");
					else if(o.op.equals("~"))ans.append("\tnot ");
					else if(o.op.equals("neg"))ans.append("\tneg ");
					else throw new Exception();
					if(!o.op.equals("!"))ans.append(varstring(cc)+"\n");
					if(cc<=4)ans.append("\tmov "+varstring(o.c)+","+regname[cc]+"\n");
				}
				else throw new Exception();
			}
		}
		reader=new BufferedReader(new FileReader("./builtin2.txt"));
		for(String line;(line=reader.readLine())!=null;)
			ans.append(line+"\n");
		for(int i=0;i<conststr.size();i++){
			ans.append("S"+(i+1)+":\n");
			ans.append("\tdb ");
			List<Integer>z=new ArrayList<Integer>();
			boolean zy=false;
			for(int j=1;j+1<conststr.get(i).length();j++){
				char w=conststr.get(i).charAt(j);
				if(zy==true){
					if(w=='n')z.add((int)'\n');
					else if(w=='\\')z.add((int)'\\');
					else if(w=='r')z.add((int)'\r');
					else if(w=='t')z.add((int)'\t');
					else if(w=='"')z.add((int)'"');
					else throw new Exception();
					zy=false;
				}
				else if(w=='\\')zy=true;
				else z.add((int)w);
			}
			ans.append(z.size()+"");
			for(int j=0;j<z.size();j++)
				ans.append(","+z.get(j));
			ans.append(",0\n");
		}
	}
	static Set<Integer>[]graph;
	static int[]labpos,vis;
	static Set<Integer>[]from;
	static boolean hasvar(Code o){
		return!(o.op=="funclab"||o.op=="label"||o.op=="jz"||o.op=="jnz"||o.op=="jmp"||o.op=="call"||o.op=="ret");
	}
	static void dfs(int u){
		vis[u]=1;
		for(int v:from[u])
			if(vis[v]==0)dfs(v);
	}
	static void regalloc(List<Code>ir)throws Exception{
		for(int i=1;i<=16;i++){
			vkind.put(i,"register");
			vnum.put(i,i);
		}
		occur=new Set[ir.size()];
		graph=new Set[vart+1];
		graph[0]=new TreeSet<Integer>();
		labpos=new int[ir.size()];
		vis=new int[ir.size()];
		from=new Set[ir.size()];
		for(int i=0;i<ir.size();i++){
			occur[i]=new TreeSet<Integer>();
			from[i]=new TreeSet<Integer>();
			if(ir.get(i).op=="label")
				labpos[ir.get(i).c]=i;
		}
		for(int i=0;i<ir.size();i++){
			int j=i+1;
			while(j<ir.size()&&ir.get(j).op!="funclab")j++;
			graph[0].clear();
			for(int k=i+1;k<j;k++){
				Code o=ir.get(k);
				if(!hasvar(o))continue;
				int v[]={o.c,o.a,o.b};
				for(int w:v)
					if(w!=0&&vkind.get(w)==null)
						graph[0].add(w);
			}
			int z=0;
			if(graph[0].size()*(j-i)<10000000){
				for(int v:graph[0]){
					graph[v]=new TreeSet<Integer>();
					for(int k=i+1;k<j;k++){
						vis[k]=0;
						from[k].clear();
					}
					for(int k=i+1;k<j;k++){
						Code o=ir.get(k);
						if(hasvar(o)&&o.c==v)
							continue;
						if(o.op!="ret"&&o.op!="jmp"&&o.op!="")
							from[k+1].add(k);
						if(o.op=="jz"||o.op=="jnz"||o.op=="jmp")
							from[labpos[o.c]+1].add(k);
					}
					for(int k=i+1;k<j;k++)
						if(hasvar(ir.get(k))&&(ir.get(k).a==v||ir.get(k).b==v)){
							dfs(k);
						}
					for(int k=i+1;k<j;k++)
						if(vis[k]==1){
							occur[k].add(v);
						}
				}
				for(int k=i+1;k<j;k++)
					for(int p:occur[k])
						for(int q:occur[k])
							if(p!=q)graph[p].add(q);
				List<Integer>stack=new ArrayList<Integer>();
				for(;;){
					if(graph[0].size()==0)break;
					int vv=0,sz=2000000000;
					for(int v:graph[0])
						if(graph[v].size()<sz)
							sz=graph[vv=v].size();
					if(sz<8)stack.add(vv);
					else{
						sz=0;
						for(int v:graph[0])
							if(graph[v].size()>sz)
								sz=graph[vv=v].size();
						vkind.put(vv,"stack");
						vnum.put(vv,8*(++z));
					}
					for(int v:graph[vv])
						graph[v].remove(vv);
					graph[0].remove(vv);
				}
				for(;stack.size()!=0;){
					int[]used=new int[17];
					int u=stack.get(stack.size()-1);
					stack.remove(stack.size()-1);
					for(int v:graph[u])
						if(vkind.get(v)=="register")
							used[(int)vnum.get(v)]=1;
					for(int v=9;v<=16;v++)
						if(used[v]==0)used[++used[0]]=v;
					vkind.put(u,"register");
					vnum.put(u,used[rand.nextInt(used[0])+1]);
				}
				for(int k=i;k<j;k++){
					Code o=ir.get(k);
					if(hasvar(o)&&(vkind.get(o.c)=="register"||vkind.get(o.c)=="stack")&&o.c>16&&!occur[k+1].contains(o.c))
						ir.set(k,new Code("",0,0,0));
					if(o.op=="mov"&&varstring(o.c).equals(varstring(o.a)))
						ir.set(k,new Code("",0,0,0));
				}
			}
			else for(int v:graph[0]){
				vkind.put(v,"stack");
				vnum.put(v,8*(++z));
			}
			ir.get(i).a=8*z+8;
			i=j-1;
		}
	}
	public static void main(String[] args)throws IOException,Exception{
//		System.setErr(null);
		InputStream is = new FileInputStream("test/test.txt");
		emxstarParser parser = new emxstarParser(new CommonTokenStream(new emxstarLexer(new ANTLRInputStream(is))));
		Node root = visit(parser.program());
		check(root);
		/*
		for (Code x : root.ir){
			System.out.printf(x.op+"\t%d\t%d\t%d\n",x.c,x.a,x.b);
		}
		*/
		regalloc(root.ir);
		codegen(root.ir);
		System.setOut(new PrintStream(new FileOutputStream(new File("./test/test.asm"))));
		System.out.print(ans);
	}
}


