// MIT © 2017 azu
// Unist spec
// https://github.com/syntax-tree/unist
export interface Node {
    type: string;
    data?: Data;
    position?: Position;

    [index: string]: any;
}

export interface Data {
    [index: string]: any;
}

export interface Position {
    start: Point;
    end: Point;
    index?: number;
}

export interface Point {
    line: number;
    column: number;
    offset?: number;
}

/**
 * Nodes containing a value extend the abstract interface Text (Node).
 */
export interface Text extends Node {
    value: string;
}

export interface Parent extends Node {
    children: Array<Node | Parent>;
}
