package dolda.jglob;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.annotation.*;

public class Loader {
    private final Class<? extends Annotation> an;
    private final ClassLoader cl;
    private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();

    private Loader(Class<? extends Annotation> annotation, ClassLoader loader) {
	this.an = annotation;
	this.cl = loader;
    }

    public Iterable<String> names() {
	return(new Iterable<String>() {
		public Iterator<String> iterator() {
		    return(new Iterator<String>() {
			    private Enumeration<URL> rls;
			    private Iterator<String> cur = null;

			    private Iterator<String> parse(URL url) {
				try {
				    List<String> buf = new LinkedList<String>();
				    InputStream in = url.openStream();
				    try {
					BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"));
					String ln;
					while((ln = r.readLine()) != null) {
					    ln = ln.trim();
					    if(ln.length() < 1)
						continue;
					    buf.add(ln);
					}
					return(buf.iterator());
				    } finally {
					in.close();
				    }
				} catch(IOException e) {
				    throw(new GlobAccessException(e));
				}
			    }

			    public boolean hasNext() {
				if((cur == null) || !cur.hasNext()) {
				    if(rls == null) {
					try {
					    rls = cl.getResources("META-INF/glob/" + an.getName());
					} catch(IOException e) {
					    throw(new GlobAccessException(e));
					}
				    }
				    if(!rls.hasMoreElements())
					return(false);
				    URL u = rls.nextElement();
				    cur = parse(u);
				}
				return(true);
			    }

			    public String next() {
				if(!hasNext())
				    throw(new NoSuchElementException());
				String ret = cur.next();
				return(ret);
			    }

			    public void remove() {throw(new UnsupportedOperationException());}
			});
		}
	    });
    }

    public Iterable<Class<?>> classes() {
	return(new Iterable<Class<?>>() {
		public Iterator<Class<?>> iterator() {
		    return(new Iterator<Class<?>>() {
			    private final Iterator<String> names = names().iterator();
			    private Class<?> n = null;

			    public boolean hasNext() {
				while(n == null) {
				    if(!names.hasNext())
					return(false);
				    String nm = names.next();
				    Class<?> c;
				    try {
					c = cl.loadClass(nm);
				    } catch(ClassNotFoundException e) {
					continue;
				    }
				    if(c.getAnnotation(an) == null)
					continue;
				    n = c;
				}
				return(true);
			    }

			    public Class<?> next() {
				if(!hasNext())
				    throw(new NoSuchElementException());
				Class<?> r = n;
				n = null;
				return(r);
			    }

			    public void remove() {throw(new UnsupportedOperationException());}
			});
		}
	    });
    }

    public Iterable<?> instances() {
	return(new Iterable<Object>() {
		public Iterator<Object> iterator() {
		    return(new Iterator<Object>() {
			    private final Iterator<Class<?>> classes = classes().iterator();
			    private Object n = null;

			    public boolean hasNext() {
				while(n == null) {
				    if(!classes.hasNext())
					return(false);
				    Class<?> cl = classes.next();
				    Object inst;
				    try {
					inst = cl.newInstance();
				    } catch(InstantiationException e) {
					throw(new GlobInstantiationException(e));
				    } catch(IllegalAccessException e) {
					throw(new GlobInstantiationException(e));
				    }
				    n = inst;
				}
				return(true);
			    }

			    public Object next() {
				if(!hasNext())
				    throw(new NoSuchElementException());
				Object r = n;
				n = null;
				return(r);
			    }

			    public void remove() {throw(new UnsupportedOperationException());}
			});
		}
	    });
    }

    public static Loader get(Class<? extends Annotation> annotation, ClassLoader loader) {
	return(new Loader(annotation, loader));
    }

    public static Loader get(Class<? extends Annotation> annotation) {
	return(get(annotation, annotation.getClassLoader()));
    }
}
