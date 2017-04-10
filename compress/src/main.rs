use std::fs::File;
use std::io::prelude::*;
use std::io::BufWriter;
use std::io::BufReader;
use std::collections::hash_map::HashMap;
use std::collections::hash_map;
use std::env;

extern crate byteorder;
use byteorder::{BigEndian, WriteBytesExt};


// 38 is where the pointers begin ~~~~
fn insert<Inner: Iterator<Item=i8>, W: Write>(w: &mut W,map: &mut HashMap<(u32, u32), u32>, it: &mut std::iter::Peekable<Inner>, depth: u8, el_w: &mut usize) -> u32 {
    if let None = it.peek() {
        return 0;
    }

    if depth == 0 {
        if let Some(i) = it.next() {
            (19 + i/2) as u32
        }
        else {
            0 // There's nothing
        }
    }
    else {
        let a = insert(w, map, it, depth-1, el_w);
        let b = insert(w, map, it, depth-1, el_w);
        if a < 38 && a==b {
            a
        }
        else {
            let pair = (a,b);
            match map.entry(pair) {
                hash_map::Entry::Occupied(occupied) => *occupied.get(),
                hash_map::Entry::Vacant(vacant) => {
                    w.write_u32::<BigEndian>(a).unwrap();
                    w.write_u32::<BigEndian>(b).unwrap();
                    let r = (*el_w) as u32;
                    *el_w += 1;
                    vacant.insert(r);
                    r
                }
            }
        }
    }
}


fn main() {

    let input = File::open(env::args().nth(1).unwrap()).unwrap();
    let input = BufReader::new(input);

    let output = File::create(env::args().nth(2).unwrap()).unwrap();
    let mut output = BufWriter::new(output);

    let mut it = input.bytes()
        .map(|i| i.unwrap() as i8).peekable();

    let mut map: HashMap<(u32, u32), u32> = HashMap::new();

    let mut depth = 0;
    let mut prev = (19 + it.next().unwrap()/2) as u32;
    let mut el_w = 0;
    while let Some(_) = it.peek() {
        let next = insert(&mut output, &mut map, &mut it, depth, &mut el_w);
        let pair = (prev, next);
        prev = match map.entry(pair) {
            hash_map::Entry::Occupied(occupied) => *occupied.get(),
            hash_map::Entry::Vacant(vacant) => {
                output.write_u32::<BigEndian>(prev).unwrap();
                output.write_u32::<BigEndian>(next).unwrap();
                let r = (el_w + 38) as u32;
                el_w += 1;
                vacant.insert(r);
                r
            }
        };
        println!("FInished depth {}", depth);
        println!("{}", el_w * 8);
        depth += 1;
    }
}

